package com.myorg;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.autoscaling.RequestCountScalingProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddApplicationTargetsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.constructs.Construct;

import java.util.List;

public class ApplicationLoadBalancerStack extends Stack {

  private static final String USER_DATA_CONTENT = """
    #!/bin/bash
    dnf -y upgrade
    dnf -y install httpd
    systemctl start httpd
    systemctl enable httpd
    TOKEN=$(curl -s -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
    META="http://169.254.169.254/latest/meta-data"
    H="-H X-aws-ec2-metadata-token:$TOKEN"
    INSTANCE_ID=$(curl -s $H $META/instance-id)
    AZ=$(curl -s $H $META/placement/availability-zone)
    INSTANCE_TYPE=$(curl -s $H $META/instance-type)
    ARCH=$(uname -m)
    AMI_ID=$(curl -s $H $META/ami-id)
    KERNEL=$(uname -r)
    HTTPD_VER=$(httpd -v | head -1 | awk '{print $3}')
    cat > /var/www/html/index.html <<EOF
    <!DOCTYPE html><html><head><meta charset="utf-8"><title>ALB Instance</title>
    <style>
    *{margin:0;padding:0;box-sizing:border-box}
    body{background:#1a1a2e;color:#e0e0e0;font-family:system-ui,sans-serif;display:flex;justify-content:center;align-items:center;min-height:100vh}
    .card{background:#16213e;border-radius:12px;padding:2rem;max-width:480px;width:90%;box-shadow:0 8px 32px rgba(0,0,0,.4)}
    h1{color:#00d4ff;margin-bottom:1.5rem;font-size:1.4rem;text-align:center}
    .row{display:flex;justify-content:space-between;padding:.6rem 0;border-bottom:1px solid #1a1a3e}
    .label{color:#8892b0;font-size:.85rem}.value{color:#ccd6f6;font-weight:600;font-size:.85rem}
    </style></head><body><div class="card"><h1>Application Load Balancer</h1>
    <div class="row"><span class="label">Instance ID</span><span class="value">$INSTANCE_ID</span></div>
    <div class="row"><span class="label">Availability Zone</span><span class="value">$AZ</span></div>
    <div class="row"><span class="label">Instance Type</span><span class="value">$INSTANCE_TYPE</span></div>
    <div class="row"><span class="label">Architecture</span><span class="value">$ARCH</span></div>
    <div class="row"><span class="label">AMI ID</span><span class="value">$AMI_ID</span></div>
    <div class="row"><span class="label">Kernel</span><span class="value">$KERNEL</span></div>
    <div class="row"><span class="label">Web Server</span><span class="value">$HTTPD_VER</span></div>
    <div class="row"><span class="label">CDK Construct</span><span class="value">AutoScalingGroup</span></div>
    </div></body></html>
    EOF
    """;

  public ApplicationLoadBalancerStack(final Construct scope, final String id, final StackProps props) {
    super(scope, id, props);
    var vpc = Vpc.Builder.create(this, "VPC")
      .restrictDefaultSecurityGroup(false)
      .build();
    var autoScalingGroup = AutoScalingGroup.Builder.create(this, "ASG")
      .vpc(vpc)
      .instanceType(InstanceType.of(InstanceClass.BURSTABLE4_GRAVITON, InstanceSize.MICRO))
      .machineImage(AmazonLinuxImage.Builder.create()
        .generation(AmazonLinuxGeneration.AMAZON_LINUX_2023)
        .cpuType(AmazonLinuxCpuType.ARM_64)
        .build())
      .userData(UserData.custom(USER_DATA_CONTENT))
      .desiredCapacity(2)
      .maxCapacity(3)
      .minCapacity(1)
      .requireImdsv2(true)
      .build();
    var alb = ApplicationLoadBalancer.Builder.create(this, "LB")
      .vpc(vpc)
      .internetFacing(Boolean.TRUE)
      .build();
    var listener = alb.addListener("Listener", ApplicationListenerProps.builder()
      .port(80)
      .loadBalancer(alb)
      .build());
    listener.addTargets("Target", AddApplicationTargetsProps.builder()
      .port(80)
      .targets(List.of(autoScalingGroup))
      .build());
    listener.getConnections().allowDefaultPortFromAnyIpv4("Open to the world");
    autoScalingGroup.scaleOnRequestCount("AModestLoad", RequestCountScalingProps.builder()
      .targetRequestsPerMinute(60)
      .build());
    CfnOutput.Builder.create(this, "ApplicationLoadBalancerURL")
      .value(listener.getLoadBalancer().getLoadBalancerDnsName())
      .description("The DNS of the application load balancer.")
      .build();
  }
}
