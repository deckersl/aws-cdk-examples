package com.myorg;

import software.constructs.Construct;

import java.util.List;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.services.ec2.AmazonLinux2023ImageSsmParameterProps;
import software.amazon.awscdk.services.ec2.AmazonLinuxCpuType;
import software.amazon.awscdk.services.ec2.Instance;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.LaunchTemplateHttpTokens;
import software.amazon.awscdk.services.ec2.MachineImage;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;

public class Ec2InstanceStack extends Stack {
    public Ec2InstanceStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public Ec2InstanceStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Vpc vpc = Vpc.Builder.create(this, "Vpc")
                .natGateways(0)
                .subnetConfiguration(List.of(SubnetConfiguration.builder()
                        .name("public")
                        .subnetType(SubnetType.PUBLIC)
                        .build()))
                .build();

        SecurityGroup sg = SecurityGroup.Builder.create(this, "InstanceSg")
                .vpc(vpc)
                .allowAllOutbound(true)
                .build();
        sg.addIngressRule(Peer.anyIpv4(), Port.tcp(80), "HTTP");

        Role role = Role.Builder.create(this, "InstanceRole")
                .assumedBy(new ServicePrincipal("ec2.amazonaws.com"))
                .build();
        role.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AmazonSSMManagedInstanceCore"));

        Instance instance = Instance.Builder.create(this, "Ec2Instance")
                .instanceType(InstanceType.of(software.amazon.awscdk.services.ec2.InstanceClass.T4G,
                        software.amazon.awscdk.services.ec2.InstanceSize.NANO))
                .machineImage(MachineImage.latestAmazonLinux2023(AmazonLinux2023ImageSsmParameterProps.builder()
                        .cpuType(AmazonLinuxCpuType.ARM_64)
                        .build()))
                .role(role)
                .vpc(vpc)
                .securityGroup(sg)
                .requireImdsv2(true)
                .build();

        instance.addUserData(
                "#!/bin/bash",
                "yum install -y httpd",
                "TOKEN=$(curl -s -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 300')",
                "INSTANCE_ID=$(curl -s -H \"X-aws-ec2-metadata-token: $TOKEN\" http://169.254.169.254/latest/meta-data/instance-id)",
                "AZ=$(curl -s -H \"X-aws-ec2-metadata-token: $TOKEN\" http://169.254.169.254/latest/meta-data/placement/availability-zone)",
                "INSTANCE_TYPE=$(curl -s -H \"X-aws-ec2-metadata-token: $TOKEN\" http://169.254.169.254/latest/meta-data/instance-type)",
                "LOCAL_IP=$(curl -s -H \"X-aws-ec2-metadata-token: $TOKEN\" http://169.254.169.254/latest/meta-data/local-ipv4)",
                "PUBLIC_IP=$(curl -s -H \"X-aws-ec2-metadata-token: $TOKEN\" http://169.254.169.254/latest/meta-data/public-ipv4)",
                "cat > /var/www/html/index.html << 'HTMLEOF'",
                "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>EC2 Instance Info</title>",
                "<style>body{font-family:system-ui,sans-serif;max-width:600px;margin:60px auto;background:#f0f4f8;color:#1a202c}",
                ".card{background:#fff;border-radius:12px;padding:32px;box-shadow:0 4px 12px rgba(0,0,0,.08)}",
                "h1{color:#2b6cb0;margin-top:0}table{width:100%;border-collapse:collapse}",
                "td{padding:10px 12px;border-bottom:1px solid #e2e8f0}td:first-child{font-weight:600;width:40%}</style></head>",
                "<body><div class=\"card\"><h1>&#x1F4E6; EC2 Instance Info</h1><table>",
                "HTMLEOF",
                "cat >> /var/www/html/index.html << EOF",
                "<tr><td>Instance ID</td><td>${INSTANCE_ID}</td></tr>",
                "<tr><td>Instance Type</td><td>${INSTANCE_TYPE}</td></tr>",
                "<tr><td>Availability Zone</td><td>${AZ}</td></tr>",
                "<tr><td>Private IP</td><td>${LOCAL_IP}</td></tr>",
                "<tr><td>Public IP</td><td>${PUBLIC_IP}</td></tr>",
                "EOF",
                "cat >> /var/www/html/index.html << 'HTMLEOF'",
                "</table></div></body></html>",
                "HTMLEOF",
                "systemctl enable httpd",
                "systemctl start httpd"
        );

        CfnOutput.Builder.create(this, "InstancePublicIp")
                .value(instance.getInstancePublicIp())
                .build();
    }
}
