package main

import (
	"github.com/aws/aws-cdk-go/awscdk/v2"
	ec2 "github.com/aws/aws-cdk-go/awscdk/v2/awsec2"
	iam "github.com/aws/aws-cdk-go/awscdk/v2/awsiam"
	"github.com/aws/constructs-go/constructs/v10"
	"github.com/aws/jsii-runtime-go"
)

type Ec2InstanceStackProps struct {
	awscdk.StackProps
}

func NewEc2InstanceStack(scope constructs.Construct, id string, props *Ec2InstanceStackProps) awscdk.Stack {
	var sprops awscdk.StackProps
	if props != nil {
		sprops = props.StackProps
	}
	stack := awscdk.NewStack(scope, &id, &sprops)

	vpc := ec2.NewVpc(stack, jsii.String("VPC"), &ec2.VpcProps{
		NatGateways: jsii.Number(0),
		SubnetConfiguration: &[]*ec2.SubnetConfiguration{{
			CidrMask:   jsii.Number(24),
			Name:       jsii.String("public"),
			SubnetType: ec2.SubnetType_PUBLIC,
		}},
	})

	role := iam.NewRole(stack, jsii.String("InstanceSSM"), &iam.RoleProps{
		AssumedBy: iam.NewServicePrincipal(jsii.String("ec2.amazonaws.com"), nil),
	})
	role.AddManagedPolicy(iam.ManagedPolicy_FromAwsManagedPolicyName(jsii.String("AmazonSSMManagedInstanceCore")))

	sg := ec2.NewSecurityGroup(stack, jsii.String("SG"), &ec2.SecurityGroupProps{
		Vpc:              vpc,
		AllowAllOutbound: jsii.Bool(true),
	})
	sg.AddIngressRule(ec2.Peer_AnyIpv4(), ec2.Port_Tcp(jsii.Number(80)), jsii.String("HTTP"), nil)

	instance := ec2.NewInstance(stack, jsii.String("Instance"), &ec2.InstanceProps{
		InstanceType: ec2.NewInstanceType(jsii.String("t4g.micro")),
		MachineImage: ec2.MachineImage_LatestAmazonLinux2023(&ec2.AmazonLinux2023ImageSsmParameterProps{
			CpuType: ec2.AmazonLinuxCpuType_ARM_64,
		}),
		Vpc:                      vpc,
		Role:                     role,
		SecurityGroup:            sg,
		RequireImdsv2:            jsii.Bool(true),
		VpcSubnets:               &ec2.SubnetSelection{SubnetType: ec2.SubnetType_PUBLIC},
		AssociatePublicIpAddress: jsii.Bool(true),
	})

	instance.UserData().AddCommands(
		jsii.String("#!/bin/bash"),
		jsii.String("dnf install -y httpd"),
		jsii.String("systemctl enable httpd"),
		jsii.String("TOKEN=$(curl -s -X PUT http://169.254.169.254/latest/api/token -H \"X-aws-ec2-metadata-token-ttl-seconds: 300\")"),
		jsii.String("H(){ curl -s -H \"X-aws-ec2-metadata-token: $TOKEN\" http://169.254.169.254/latest/meta-data/$1; }"),
		jsii.String("cat > /var/www/html/index.html <<'ENDOFHTML'\n"+
			"<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>EC2 Info</title>\n"+
			"<style>*{margin:0;padding:0;box-sizing:border-box}body{background:#1a1a2e;color:#e0e0e0;font-family:system-ui;display:flex;justify-content:center;align-items:center;min-height:100vh}.card{background:#16213e;border-radius:12px;padding:2rem;max-width:480px;width:90%;box-shadow:0 8px 32px rgba(0,0,0,.4)}.card h1{color:#0f9;margin-bottom:1rem;font-size:1.5rem}.row{display:flex;justify-content:space-between;padding:.5rem 0;border-bottom:1px solid #1a1a2e}.row:last-child{border:none}.label{color:#888}.value{color:#0f9;font-weight:600}</style>\n"+
			"</head><body><div class=\"card\"><h1>&#x1F4E6; EC2 Instance Info</h1><div id=\"info\"></div></div>\n"+
			"<script>const d=document.getElementById('info');const items={\n"+
			"ENDOFHTML"),
		jsii.String("echo \"'Instance ID':'$(H instance-id)',\" >> /var/www/html/index.html"),
		jsii.String("echo \"'Instance Type':'$(H instance-type)',\" >> /var/www/html/index.html"),
		jsii.String("echo \"'Availability Zone':'$(H placement/availability-zone)',\" >> /var/www/html/index.html"),
		jsii.String("echo \"'Private IP':'$(H local-ipv4)',\" >> /var/www/html/index.html"),
		jsii.String("echo \"'Public IP':'$(H public-ipv4)',\" >> /var/www/html/index.html"),
		jsii.String("echo \"'AMI ID':'$(H ami-id)',\" >> /var/www/html/index.html"),
		jsii.String("echo \"'Architecture':'$(uname -m)',\" >> /var/www/html/index.html"),
		jsii.String("cat >> /var/www/html/index.html <<'ENDOFHTML'\n"+
			"};Object.entries(items).forEach(([k,v])=>{d.innerHTML+=`<div class=\"row\"><span class=\"label\">${k}</span><span class=\"value\">${v}</span></div>`})</script></body></html>\n"+
			"ENDOFHTML"),
		jsii.String("systemctl start httpd"),
	)

	awscdk.NewCfnOutput(stack, jsii.String("InstancePublicIP"), &awscdk.CfnOutputProps{
		Value: instance.InstancePublicIp(),
	})

	return stack
}

func main() {
	defer jsii.Close()
	app := awscdk.NewApp(nil)
	NewEc2InstanceStack(app, "Ec2InstanceStack", &Ec2InstanceStackProps{})
	app.Synth(nil)
}
