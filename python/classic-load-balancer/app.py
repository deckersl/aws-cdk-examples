from aws_cdk import (
    aws_autoscaling as autoscaling,
    aws_ec2 as ec2,
    aws_elasticloadbalancing as elb,
    App, CfnOutput, Stack
)


class LoadBalancerStack(Stack):
    def __init__(self, app: App, id: str, **kwargs) -> None:
        super().__init__(app, id, **kwargs)

        vpc = ec2.Vpc(self, "VPC", nat_gateways=0)

        user_data = ec2.UserData.for_linux()
        user_data.add_commands(
            "dnf install -y httpd",
            "TOKEN=$(curl -s -X PUT http://169.254.169.254/latest/api/token -H \"X-aws-ec2-metadata-token-ttl-seconds: 300\")",
            "INSTANCE_ID=$(curl -s -H \"X-aws-ec2-metadata-token: $TOKEN\" http://169.254.169.254/latest/meta-data/instance-id)",
            "AZ=$(curl -s -H \"X-aws-ec2-metadata-token: $TOKEN\" http://169.254.169.254/latest/meta-data/placement/availability-zone)",
            "cat > /var/www/html/index.html <<'PAGE'\n"
            "<!DOCTYPE html><html><head><title>Classic LB Demo</title>"
            "<style>body{font-family:system-ui;display:flex;justify-content:center;align-items:center;min-height:100vh;margin:0;background:linear-gradient(135deg,#667eea,#764ba2);color:#fff}"
            ".card{background:rgba(255,255,255,.15);backdrop-filter:blur(10px);border-radius:16px;padding:2rem 3rem;text-align:center}"
            "h1{margin:0 0 1rem}p{margin:.5rem 0;font-size:1.1rem}</style></head>"
            "<body><div class='card'><h1>&#9889; Classic Load Balancer</h1>"
            "<p><b>Instance:</b> INSTANCE_PLACEHOLDER</p>"
            "<p><b>AZ:</b> AZ_PLACEHOLDER</p></div></body></html>\nPAGE",
            "sed -i \"s/INSTANCE_PLACEHOLDER/$INSTANCE_ID/;s/AZ_PLACEHOLDER/$AZ/\" /var/www/html/index.html",
            "systemctl enable --now httpd",
        )

        asg = autoscaling.AutoScalingGroup(
            self, "ASG",
            vpc=vpc,
            vpc_subnets=ec2.SubnetSelection(subnet_type=ec2.SubnetType.PUBLIC),
            instance_type=ec2.InstanceType.of(
                ec2.InstanceClass.BURSTABLE4_GRAVITON, ec2.InstanceSize.MICRO
            ),
            machine_image=ec2.MachineImage.latest_amazon_linux2023(
                cpu_type=ec2.AmazonLinuxCpuType.ARM_64,
            ),
            require_imdsv2=True,
            user_data=user_data,
        )

        lb = elb.LoadBalancer(
            self, "LB",
            vpc=vpc,
            internet_facing=True,
            health_check=elb.HealthCheck(port=80),
        )
        lb.add_target(asg)

        listener = lb.add_listener(external_port=80)
        listener.connections.allow_default_port_from_any_ipv4("Open to the world")

        CfnOutput(self, "LoadBalancerDNS", value=lb.load_balancer_dns_name)


app = App()
LoadBalancerStack(app, "LoadBalancerStack")
app.synth()
