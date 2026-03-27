package com.myorg;

import com.myorg.utils.PropertyLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.ec2.AmazonLinuxGeneration;
import software.amazon.awscdk.services.ec2.AmazonLinuxImage;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.LaunchTemplate;
import software.amazon.awscdk.services.ec2.MachineImage;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.UserData;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListenerRule;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationTargetGroupProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.FixedResponseOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.IApplicationLoadBalancerTarget;
import software.amazon.awscdk.services.elasticloadbalancingv2.IApplicationTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.ListenerAction;
import software.amazon.awscdk.services.elasticloadbalancingv2.ListenerCondition;
import software.amazon.awscdk.services.elasticloadbalancingv2.TargetType;
import software.constructs.Construct;

public class ALBProjectStack extends Stack {

  public ALBProjectStack(final Construct scope, final String id) {
    this(scope, id, null);
  }

  public ALBProjectStack(final Construct scope, final String id, final StackProps props) {
    super(scope, id, props);

    // property loader
    PropertyLoader propertyLoad = new PropertyLoader();

    // create ALB and all ancillary services
    Vpc vpc = Vpc.Builder.create(this, "VPC").build();

    SecurityGroup asgSg = SecurityGroup.Builder.create(this, "ASGSG")
        .vpc(vpc)
        .allowAllOutbound(true)
        .build();

    Role asgRole = Role.Builder.create(this, "ASGRole")
        .assumedBy(new ServicePrincipal("ec2.amazonaws.com"))
        .build();

    UserData userData = propertyLoad.getUserData();
    String html = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">"
        + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
        + "<title>ALB Multi-Rule Response</title><style>"
        + "*{margin:0;padding:0;box-sizing:border-box}"
        + "body{font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,sans-serif;"
        + "min-height:100vh;display:flex;align-items:center;justify-content:center;"
        + "background:linear-gradient(135deg,#0f172a 0%,#1e293b 100%);color:#e2e8f0}"
        + ".card{background:rgba(30,41,59,.85);border:1px solid rgba(148,163,184,.15);"
        + "border-radius:16px;padding:48px;max-width:600px;width:90%;text-align:center;"
        + "backdrop-filter:blur(12px);box-shadow:0 25px 50px rgba(0,0,0,.4)}"
        + "h1{font-size:1.75rem;font-weight:700;"
        + "background:linear-gradient(90deg,#38bdf8,#818cf8);"
        + "-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-bottom:8px}"
        + "p.sub{color:#94a3b8;font-size:.95rem;margin-bottom:32px}"
        + ".routes{text-align:left;margin-bottom:32px}"
        + ".routes h2{font-size:.8rem;text-transform:uppercase;letter-spacing:.1em;"
        + "color:#64748b;margin-bottom:12px}"
        + ".route{display:flex;justify-content:space-between;align-items:center;"
        + "padding:10px 14px;border-radius:8px;margin-bottom:6px;"
        + "background:rgba(15,23,42,.5);font-size:.85rem}"
        + ".route .method{color:#38bdf8;font-weight:600;font-family:monospace}"
        + ".route .path{color:#e2e8f0;font-family:monospace}"
        + ".route .host{color:#818cf8;font-size:.75rem}"
        + ".versions{border-top:1px solid rgba(148,163,184,.1);padding-top:20px;"
        + "display:grid;grid-template-columns:1fr 1fr;gap:8px;text-align:left}"
        + ".ver{font-size:.75rem;color:#64748b}.ver span{color:#94a3b8;font-weight:500}"
        + ".footer{margin-top:24px;font-size:.7rem;color:#475569}"
        + "</style></head><body><div class=\"card\">"
        + "<h1>ALB Multi-Rule Response</h1>"
        + "<p class=\"sub\">Host &amp; path-based routing demo on AWS CDK</p>"
        + "<div class=\"routes\"><h2>Configured Routes</h2>"
        + "<div class=\"route\"><span class=\"method\">GET</span>"
        + "<span class=\"path\">/production</span>"
        + "<span class=\"host\">api.mydomain.com</span></div>"
        + "<div class=\"route\"><span class=\"method\">GET</span>"
        + "<span class=\"path\">/production</span>"
        + "<span class=\"host\">mobile.mydomain.com</span></div>"
        + "<div class=\"route\"><span class=\"method\">GET</span>"
        + "<span class=\"path\">/sandbox</span>"
        + "<span class=\"host\">api.mydomain.com</span></div>"
        + "<div class=\"route\"><span class=\"method\">GET</span>"
        + "<span class=\"path\">/sandbox</span>"
        + "<span class=\"host\">mobile.mydomain.com</span></div></div>"
        + "<div class=\"versions\">"
        + "<div class=\"ver\">CDK CLI <span>2.1114.0</span></div>"
        + "<div class=\"ver\">aws-cdk-lib <span>2.244.0</span></div>"
        + "<div class=\"ver\">Java <span>21 (Corretto)</span></div>"
        + "<div class=\"ver\">Maven <span>3.9.14</span></div>"
        + "<div class=\"ver\">constructs <span>10.6.0</span></div>"
        + "<div class=\"ver\">OS <span>Amazon Linux 2023</span></div></div>"
        + "<p class=\"footer\">Deployed 2026-03-27 &bull; ALBProjectStack &bull; us-west-2</p>"
        + "</div></body></html>";
    userData.addCommands(
        "cat > /var/www/html/index.html << 'HTMLEOF'",
        html,
        "HTMLEOF",
        "service httpd start"
    );

    LaunchTemplate lt = LaunchTemplate.Builder.create(this, "LT")
        .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO))
        .machineImage(MachineImage.latestAmazonLinux2023())
        .userData(userData)
        .securityGroup(asgSg)
        .role(asgRole)
        .launchTemplateName("alb-project-lt-v2")
        .build();

    AutoScalingGroup asg =
        AutoScalingGroup.Builder.create(this, "ASG")
            .vpc(vpc)
            .launchTemplate(lt)
            .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PRIVATE_WITH_EGRESS).build())
            .build();

    ApplicationLoadBalancer lb =
        ApplicationLoadBalancer.Builder.create(this, "LB")
            .vpc(vpc)
            .internetFacing(Boolean.TRUE)
            .loadBalancerName("myalb")
            .build();

    List<IApplicationLoadBalancerTarget> targets = new ArrayList<IApplicationLoadBalancerTarget>();
    targets.add(asg);

    ApplicationTargetGroup webTargetGroup =
        new ApplicationTargetGroup(
            this,
            "MyTargetGroup",
            ApplicationTargetGroupProps.builder()
                .vpc(vpc)
                .targetType(TargetType.INSTANCE)
                .targets(targets)
                .port(80)
                .protocol(ApplicationProtocol.HTTP)
                .build());

    List<IApplicationTargetGroup> targetGroups = new ArrayList<IApplicationTargetGroup>();
    targetGroups.add(webTargetGroup);

    // default listener
    ApplicationListener http =
        ApplicationListener.Builder.create(this, "HTTP")
            .port(80)
            .protocol(ApplicationProtocol.HTTP)
            .open(true)
            .loadBalancer(lb)
            .defaultTargetGroups(targetGroups)
            .build();

    // adding application listern rules
    ApplicationListenerRule alrProdApi =
        ApplicationListenerRule.Builder.create(this, "prodApi")
            .conditions(Arrays.asList(
                ListenerCondition.pathPatterns(Arrays.asList("/production")),
                ListenerCondition.hostHeaders(Arrays.asList(propertyLoad.getRestAPIHostHeader())))
            )
            .priority(1)
            .listener(http)
            .build();

    ApplicationListenerRule alrProdM =
        ApplicationListenerRule.Builder.create(this, "prodMobile")
            .conditions(Arrays.asList(
                ListenerCondition.pathPatterns(Arrays.asList("/production")),
                ListenerCondition.hostHeaders(Arrays.asList(propertyLoad.getRestMobileHostHeader())))
            )
            .priority(2)
            .listener(http)
            .build();

    ApplicationListenerRule alrSandboxApi =
        ApplicationListenerRule.Builder.create(this, "sandboxApi")
            .conditions(Arrays.asList(
                ListenerCondition.pathPatterns(Arrays.asList("/sandbox")),
                ListenerCondition.hostHeaders(Arrays.asList(propertyLoad.getRestAPIHostHeader())))
            )
            .priority(3)
            .listener(http)
            .build();

    ApplicationListenerRule alrSandboxM =
        ApplicationListenerRule.Builder.create(this, "sandboxMobile")
            .conditions(Arrays.asList(
                ListenerCondition.pathPatterns(Arrays.asList("/sandbox")),
                ListenerCondition.hostHeaders(Arrays.asList(propertyLoad.getRestMobileHostHeader())))
            )
            .priority(4)
            .listener(http)
            .build();

    // adding fixed responses
    alrProdApi.configureAction(
        ListenerAction.fixedResponse(200, FixedResponseOptions.builder()
        .contentType("application/json")
        .messageBody(propertyLoad.getProdApiMessageBody())
        .build()));

    alrProdM.configureAction(
        ListenerAction.fixedResponse(200, FixedResponseOptions.builder()
        .contentType("application/json")
        .messageBody(propertyLoad.getProdMobileMessageBody())
        .build()));

    alrSandboxApi.configureAction(
        ListenerAction.fixedResponse(200, FixedResponseOptions.builder()
        .contentType("application/json")
        .messageBody(propertyLoad.getSandboxApiMessageBody())
        .build()));

    alrSandboxM.configureAction(
        ListenerAction.fixedResponse(200, FixedResponseOptions.builder()
        .contentType("application/json")
        .messageBody(propertyLoad.getSandboxMobileMessageBody())
        .build()));
  }
}
