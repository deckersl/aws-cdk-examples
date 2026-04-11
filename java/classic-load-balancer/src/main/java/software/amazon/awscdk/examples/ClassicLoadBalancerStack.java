package software.amazon.awscdk.examples;

import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.elasticloadbalancing.*;

class ClassicLoadBalancerStack extends Stack {
    public ClassicLoadBalancerStack(final Construct parent, final String name) {
        this(parent, name, null);
    }

    public ClassicLoadBalancerStack(final Construct parent, final String name, final StackProps props) {
        super(parent, name, props);

        Vpc vpc = new Vpc(this, "VPC");

        UserData userData = UserData.forLinux();
        userData.addCommands(
            "yum install -y httpd",
            "systemctl enable httpd",
            "TOKEN=$(curl -s -X PUT \"http://169.254.169.254/latest/api/token\" -H \"X-aws-ec2-metadata-token-ttl-seconds: 300\")",
            "INSTANCE_ID=$(curl -s -H \"X-aws-ec2-metadata-token: $TOKEN\" http://169.254.169.254/latest/meta-data/instance-id)",
            "AZ=$(curl -s -H \"X-aws-ec2-metadata-token: $TOKEN\" http://169.254.169.254/latest/meta-data/placement/availability-zone)",
            "INSTANCE_TYPE=$(curl -s -H \"X-aws-ec2-metadata-token: $TOKEN\" http://169.254.169.254/latest/meta-data/instance-type)",
            "LOCAL_IP=$(curl -s -H \"X-aws-ec2-metadata-token: $TOKEN\" http://169.254.169.254/latest/meta-data/local-ipv4)",
            "cat > /var/www/html/index.html << 'HTMLEOF'\n"
            + "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>Sandbox Instance</title>\n"
            + "<style>*{margin:0;padding:0;box-sizing:border-box}body{background:#1a1a2e;color:#e0e0e0;font-family:system-ui,sans-serif;display:flex;justify-content:center;align-items:center;min-height:100vh}\n"
            + ".card{background:#16213e;border-radius:12px;padding:2rem;max-width:480px;width:90%;box-shadow:0 8px 32px rgba(0,0,0,.4)}\n"
            + "h1{color:#0f3460;background:#e94560;padding:.5rem 1rem;border-radius:8px;text-align:center;margin-bottom:1.5rem;color:#fff}\n"
            + ".row{display:flex;justify-content:space-between;padding:.6rem 0;border-bottom:1px solid #1a1a2e}.label{color:#a0a0b0}.value{color:#e94560;font-weight:600}</style></head>\n"
            + "<body><div class=\"card\"><h1>&#x1F680; Sandbox Instance</h1>\n"
            + "<div class=\"row\"><span class=\"label\">Instance ID</span><span class=\"value\" id=\"iid\"></span></div>\n"
            + "<div class=\"row\"><span class=\"label\">AZ</span><span class=\"value\" id=\"az\"></span></div>\n"
            + "<div class=\"row\"><span class=\"label\">Type</span><span class=\"value\" id=\"itype\"></span></div>\n"
            + "<div class=\"row\"><span class=\"label\">Local IP</span><span class=\"value\" id=\"lip\"></span></div>\n"
            + "</div></body></html>\n"
            + "HTMLEOF",
            "sed -i \"s|id=\\\"iid\\\"></|id=\\\"iid\\\">$INSTANCE_ID</|\" /var/www/html/index.html",
            "sed -i \"s|id=\\\"az\\\"></|id=\\\"az\\\">$AZ</|\" /var/www/html/index.html",
            "sed -i \"s|id=\\\"itype\\\"></|id=\\\"itype\\\">$INSTANCE_TYPE</|\" /var/www/html/index.html",
            "sed -i \"s|id=\\\"lip\\\"></|id=\\\"lip\\\">$LOCAL_IP</|\" /var/www/html/index.html",
            "systemctl start httpd"
        );

        SecurityGroup sg = SecurityGroup.Builder.create(this, "LTSG")
                .vpc(vpc)
                .build();

        LaunchTemplate lt = LaunchTemplate.Builder.create(this, "LT")
                .machineImage(MachineImage.latestAmazonLinux2023(AmazonLinux2023ImageSsmParameterProps.builder()
                        .cpuType(AmazonLinuxCpuType.ARM_64)
                        .build()))
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE4_GRAVITON, InstanceSize.MICRO))
                .userData(userData)
                .httpTokens(LaunchTemplateHttpTokens.REQUIRED)
                .securityGroup(sg)
                .build();

        AutoScalingGroup asg = AutoScalingGroup.Builder.create(this, "ASG")
                .vpc(vpc)
                .launchTemplate(lt)
                .build();

        HealthCheck healthCheck = HealthCheck.builder().port(80).build();

        LoadBalancer lb = LoadBalancer.Builder.create(this, "LB")
                .vpc(vpc)
                .internetFacing(Boolean.TRUE)
                .healthCheck(healthCheck)
                .build();
        lb.addTarget(asg);
        ListenerPort listenerPort = lb.addListener(LoadBalancerListener.builder().externalPort(80).build());
        listenerPort.getConnections().allowDefaultPortFromAnyIpv4("Open to the world");
    }
}
