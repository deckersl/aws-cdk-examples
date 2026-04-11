#!/usr/bin/env python3
from aws_cdk import (
    aws_autoscaling as autoscaling,
    aws_ec2 as ec2,
    aws_elasticloadbalancingv2 as elbv2,
    App, CfnOutput, Stack
)


class LoadBalancerStack(Stack):
    def __init__(self, app: App, id: str) -> None:
        super().__init__(app, id)

        vpc = ec2.Vpc(self, "VPC")

        user_data = ec2.UserData.for_linux()
        with open("./httpd.sh", "r") as f:
            user_data.add_commands(f.read())

        sg = ec2.SecurityGroup(self, "ASGSG", vpc=vpc)

        lt = ec2.LaunchTemplate(
            self, "LT",
            instance_type=ec2.InstanceType.of(
                ec2.InstanceClass.BURSTABLE4_GRAVITON, ec2.InstanceSize.MICRO
            ),
            machine_image=ec2.MachineImage.latest_amazon_linux2023(
                cpu_type=ec2.AmazonLinuxCpuType.ARM_64,
            ),
            user_data=user_data,
            require_imdsv2=True,
            security_group=sg,
        )

        asg = autoscaling.AutoScalingGroup(
            self, "ASG",
            vpc=vpc,
            launch_template=lt,
            min_capacity=1,
            max_capacity=3,
        )

        lb = elbv2.ApplicationLoadBalancer(
            self, "LB",
            vpc=vpc,
            internet_facing=True,
        )

        listener = lb.add_listener("HttpListener", port=80)
        listener.add_targets("Target", port=80, targets=[asg])
        listener.connections.allow_default_port_from_any_ipv4("Open to the world")

        asg.scale_on_request_count("AModestLoad", target_requests_per_minute=60)

        CfnOutput(self, "LoadBalancerDNS", value=lb.load_balancer_dns_name)


app = App()
LoadBalancerStack(app, "LoadBalancerStack")
app.synth()
