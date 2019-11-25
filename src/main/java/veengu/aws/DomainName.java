package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.route53.*;
import software.amazon.awscdk.services.route53.targets.LoadBalancerTarget;

public class DomainName extends Stack {

    public DomainName(Construct scope, String id, String zoneName, String zoneId, ApplicationLoadBalancer loadBalancer) {
        super(scope, id);

        ///////////////////////////////////////////////////////////////////////////
        // Hosted Zone
        ///////////////////////////////////////////////////////////////////////////

        HostedZoneAttributes zoneAttributes = HostedZoneAttributes.builder()
                .hostedZoneId(zoneId)
                .zoneName(zoneName)
                .build();

        IHostedZone hostedZone = HostedZone.fromHostedZoneAttributes(this, "VeenguZone", zoneAttributes);

        ///////////////////////////////////////////////////////////////////////////
        // DNS Record
        ///////////////////////////////////////////////////////////////////////////

        ARecord.Builder
                .create(this, "DomainRecord")
                .zone(hostedZone)
                .target(RecordTarget.fromAlias(new LoadBalancerTarget(loadBalancer)))
                .build();
    }
}
