package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.codebuild.*;
import software.amazon.awscdk.services.codecommit.IRepository;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageProps;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeCommitSourceAction;

import java.util.List;
import java.util.Map;

import static software.amazon.awscdk.services.codebuild.BuildEnvironmentVariableType.PLAINTEXT;
import static software.amazon.awscdk.services.codebuild.LocalCacheMode.*;
import static software.amazon.awscdk.services.codepipeline.actions.CodeCommitTrigger.EVENTS;


public class PipelineStack extends Stack {

    public PipelineStack(final Construct scope, final String id, IRepository repository, String branchName) {
        super(scope, id, null);

        ///////////////////////////////////////////////////////////////////////////
        // Build Project
        ///////////////////////////////////////////////////////////////////////////

        CodeCommitSourceProps repositorySource = CodeCommitSourceProps.builder()
                .cloneDepth(1)
                .repository(repository)
                .build();

        Map<String, BuildEnvironmentVariable> environmentVariables = Map.of(
                "AWS_DEFAULT_REGION", BuildEnvironmentVariable.builder().type(PLAINTEXT).value(getRegion()).build(),
                "AWS_ACCOUNT_ID", BuildEnvironmentVariable.builder().type(PLAINTEXT).value(getAccount()).build(),
                "IMAGE_TAG", BuildEnvironmentVariable.builder().type(PLAINTEXT).value("latest").build(),
                "IMAGE_REPO_NAME", BuildEnvironmentVariable.builder().type(PLAINTEXT).value("veengu-service").build()); // TODO replace with EcrRepository url

        BuildEnvironment buildEnvironment = BuildEnvironment.builder()
                .buildImage(LinuxBuildImage.STANDARD_2_0)
                .privileged(true)
                .build();

        Project codeBuildProject = Project.Builder
                .create(this, "CodeBuilder")
                .description("Code Builder created by AWS CDK")
                .source(Source.codeCommit(repositorySource))
                .environment(buildEnvironment)
                .environmentVariables(environmentVariables)
                .cache(Cache.local(SOURCE, DOCKER_LAYER, CUSTOM))
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Source Stage
        ///////////////////////////////////////////////////////////////////////////

        Artifact sourceArtifact = Artifact.artifact("SourceArtifact");

        CodeCommitSourceAction sourceAction = CodeCommitSourceAction.Builder
                .create()
                .actionName("CodeCommitAction")
                .repository(repository)
                .branch(branchName)
                .trigger(EVENTS)
                .output(sourceArtifact)
                .build();

        StageProps sourceStage = StageProps.builder()
                .stageName("SourceStage")
                .actions(List.of(sourceAction))
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Build Stage
        ///////////////////////////////////////////////////////////////////////////

        Artifact buildArtifact = Artifact.artifact("BuildArtifact");

        CodeBuildAction buildAction = CodeBuildAction.Builder
                .create()
                .actionName("CodeBuildAction")
                .input(sourceArtifact)
                .project(codeBuildProject)
                .outputs(List.of(buildArtifact))
                .build();

        StageProps buildStage = StageProps.builder()
                .stageName("BuildStage")
                .actions(List.of(buildAction))
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Deploy Stage
        ///////////////////////////////////////////////////////////////////////////

        /*EcsDeployAction deployAction = EcsDeployAction.Builder
                .create()
                .actionName("ECSDeployAction")
                .service(baseService)
                .input(buildArtifact)
                .build();

        StageProps deployStage = StageProps.builder()
                .stageName("DeployStage")
                .actions(List.of(deployAction))
                .build();*/

        ///////////////////////////////////////////////////////////////////////////
        // Code Pipeline
        ///////////////////////////////////////////////////////////////////////////

        Pipeline.Builder
                .create(this, "CodePipeline")
                .stages(List.of(sourceStage, buildStage))
                .build();
    }
}
