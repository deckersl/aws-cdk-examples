from aws_cdk import (
    aws_ec2 as ec2,
    aws_iam as iam,
    App, Stack, CfnOutput
)
from constructs import Construct


class EC2InstanceStack(Stack):
    def __init__(self, scope: Construct, id: str, **kwargs) -> None:
        super().__init__(scope, id, **kwargs)

        vpc = ec2.Vpc(self, "VPC",
            nat_gateways=0,
            subnet_configuration=[ec2.SubnetConfiguration(name="public", subnet_type=ec2.SubnetType.PUBLIC)]
        )

        sg = ec2.SecurityGroup(self, "SG", vpc=vpc, allow_all_outbound=True)
        sg.add_ingress_rule(ec2.Peer.any_ipv4(), ec2.Port.tcp(80), "HTTP")

        role = iam.Role(self, "InstanceSSM", assumed_by=iam.ServicePrincipal("ec2.amazonaws.com"))
        role.add_managed_policy(iam.ManagedPolicy.from_aws_managed_policy_name("AmazonSSMManagedInstanceCore"))

        user_data = ec2.UserData.for_linux()
        user_data.add_commands(
            "yum install -y httpd",
            "TOKEN=$(curl -s -X PUT http://169.254.169.254/latest/api/token -H \"X-aws-ec2-metadata-token-ttl-seconds: 300\")",
            "INSTANCE_ID=$(curl -s -H \"X-aws-ec2-metadata-token: $TOKEN\" http://169.254.169.254/latest/meta-data/instance-id)",
            "AZ=$(curl -s -H \"X-aws-ec2-metadata-token: $TOKEN\" http://169.254.169.254/latest/meta-data/placement/availability-zone)",
            "INSTANCE_TYPE=$(curl -s -H \"X-aws-ec2-metadata-token: $TOKEN\" http://169.254.169.254/latest/meta-data/instance-type)",
            "cat > /var/www/html/index.html << 'ENDHTML'\n"
            "<!DOCTYPE html><html><head><title>EC2 Instance</title>"
            "<style>body{font-family:sans-serif;max-width:600px;margin:60px auto;background:#f4f4f4}"
            ".card{background:#fff;border-radius:12px;padding:32px;box-shadow:0 2px 8px rgba(0,0,0,.1)}"
            "h1{color:#232f3e}dt{font-weight:bold;color:#555;margin-top:12px}dd{margin:4px 0 0 0;color:#232f3e}</style>"
            "</head><body><div class='card'><h1>&#x1F680; EC2 Instance Info</h1><dl>"
            "<dt>Instance ID</dt><dd>PLACEHOLDER_ID</dd>"
            "<dt>Availability Zone</dt><dd>PLACEHOLDER_AZ</dd>"
            "<dt>Instance Type</dt><dd>PLACEHOLDER_TYPE</dd>"
            "</dl></div></body></html>\nENDHTML",
            "sed -i \"s/PLACEHOLDER_ID/$INSTANCE_ID/;s/PLACEHOLDER_AZ/$AZ/;s/PLACEHOLDER_TYPE/$INSTANCE_TYPE/\" /var/www/html/index.html",
            "systemctl enable httpd && systemctl start httpd",
        )

        instance = ec2.Instance(self, "Instance",
            instance_type=ec2.InstanceType("t4g.nano"),
            machine_image=ec2.MachineImage.latest_amazon_linux2023(cpu_type=ec2.AmazonLinuxCpuType.ARM_64),
            vpc=vpc,
            role=role,
            security_group=sg,
            user_data=user_data,
            require_imdsv2=True,
        )

        CfnOutput(self, "InstancePublicIp", value=instance.instance_public_ip)


app = App()
EC2InstanceStack(app, "ec2-instance")
app.synth()
