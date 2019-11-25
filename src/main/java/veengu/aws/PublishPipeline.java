package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.codebuild.*;
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


public class PublishPipeline extends Stack {

    public PublishPipeline(final Construct scope,
                           final String id,
                           final software.amazon.awscdk.services.codecommit.IRepository gitRepository,
                           final String branchName,
                           final software.amazon.awscdk.services.ecr.IRepository dockerRegistry) {
        super(scope, id);

        ///////////////////////////////////////////////////////////////////////////
        // Build Project
        ///////////////////////////////////////////////////////////////////////////

        CodeCommitSourceProps repositorySource = CodeCommitSourceProps.builder()
                .cloneDepth(1)
                .repository(gitRepository)
                .build();

        BuildEnvironment buildEnvironment = BuildEnvironment.builder()
                .buildImage(LinuxBuildImage.STANDARD_2_0)
                .privileged(true)
                .build();

        Map<String, BuildEnvironmentVariable> environmentVariables = Map.of(
                "AWS_DEFAULT_REGION", BuildEnvironmentVariable.builder().type(PLAINTEXT).value(getRegion()).build(),
                "IMAGE_NAME", BuildEnvironmentVariable.builder().type(PLAINTEXT).value(dockerRegistry.getRepositoryName()).build(),
                "REGISTRY_HOST", BuildEnvironmentVariable.builder().type(PLAINTEXT).value(getAccount() + ".dkr.ecr." + getRegion() + ".amazonaws.com").build());

        Project buildProject = Project.Builder
                .create(this, "CodeBuilder")
                .source(Source.codeCommit(repositorySource))
                .environment(buildEnvironment)
                .environmentVariables(environmentVariables)
                .cache(Cache.local(SOURCE, DOCKER_LAYER, CUSTOM))
                .build();

        dockerRegistry.grantPullPush(buildProject);

        ///////////////////////////////////////////////////////////////////////////
        // Source Stage
        ///////////////////////////////////////////////////////////////////////////

        Artifact sourceOutput = Artifact.artifact("SourceOutput");

        CodeCommitSourceAction sourceAction = CodeCommitSourceAction.Builder
                .create()
                .actionName("CodeCommitAction")
                .repository(gitRepository)
                .branch(branchName)
                .trigger(EVENTS)
                .output(sourceOutput)
                .build();

        StageProps sourceStage = StageProps.builder()
                .stageName("SourceStage")
                .actions(List.of(sourceAction))
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Build Stage
        ///////////////////////////////////////////////////////////////////////////

        Artifact buildOutput = Artifact.artifact("BuildOutput");

        CodeBuildAction buildAction = CodeBuildAction.Builder
                .create()
                .actionName("CodeBuildAction")
                .project(buildProject)
                .input(sourceOutput)
                .outputs(List.of(buildOutput))
                .build();

        StageProps buildStage = StageProps.builder()
                .stageName("BuildStage")
                .actions(List.of(buildAction))
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Deploy Stage
        ///////////////////////////////////////////////////////////////////////////

//        EcsDeployAction deployAction = EcsDeployAction.Builder
//                .create()
//                .actionName("ECSDeployAction")
//                .service(fargateService)
//                .input(buildOutput)
//                .build();
//
//        StageProps deployStage = StageProps.builder()
//                .stageName("DeployStage")
//                .actions(List.of(deployAction))
//                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Code Pipeline
        ///////////////////////////////////////////////////////////////////////////

        Pipeline.Builder
                .create(this, "CodePipeline")
                .stages(List.of(sourceStage, buildStage))
                .build();
    }
}
