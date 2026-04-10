#!/usr/bin/env node
import * as autoscaling from 'aws-cdk-lib/aws-autoscaling';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as elb from 'aws-cdk-lib/aws-elasticloadbalancing';
import * as cdk from 'aws-cdk-lib';

class LoadBalancerStack extends cdk.Stack {
  constructor(app: cdk.App, id: string, props?: cdk.StackProps) {
    super(app, id, props);

    const vpc = new ec2.Vpc(this, 'VPC');

    const sg = new ec2.SecurityGroup(this, 'InstanceSG', {
      vpc,
      description: 'Allow HTTP traffic to instances',
      allowAllOutbound: true,
    });
    sg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(80), 'HTTP');

    const userData = ec2.UserData.forLinux();
    userData.addCommands(
      'yum install -y httpd',
      'systemctl enable httpd',
      'TOKEN=$(curl -s -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 300")',
      'INSTANCE_ID=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/instance-id)',
      'AZ=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/placement/availability-zone)',
      'INSTANCE_TYPE=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/instance-type)',
      'AMI_ID=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/ami-id)',
      'ARCH=$(uname -m)',
      'KERNEL=$(uname -r)',
      'HTTPD_VERSION=$(httpd -v | head -1 | awk \'{print $3}\')',
      'cat > /var/www/html/index.html << HTML',
      '<!DOCTYPE html>',
      '<html lang="en">',
      '<head>',
      '  <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">',
      '  <title>Classic Load Balancer — CDK Example</title>',
      '  <style>',
      '    * { margin: 0; padding: 0; box-sizing: border-box; }',
      '    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: linear-gradient(135deg, #0f1923 0%, #1a2a3a 100%); color: #e0e0e0; min-height: 100vh; display: flex; align-items: center; justify-content: center; }',
      '    .card { background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); border-radius: 16px; padding: 48px; max-width: 600px; width: 90%; backdrop-filter: blur(10px); }',
      '    h1 { font-size: 1.5rem; color: #ff9900; margin-bottom: 8px; }',
      '    .subtitle { color: #888; font-size: 0.9rem; margin-bottom: 32px; }',
      '    .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }',
      '    .item { background: rgba(255,255,255,0.03); border-radius: 8px; padding: 16px; }',
      '    .label { font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.05em; color: #888; margin-bottom: 4px; }',
      '    .value { font-size: 0.95rem; color: #fff; font-family: "SF Mono", Monaco, monospace; }',
      '    .footer { margin-top: 32px; text-align: center; font-size: 0.8rem; color: #555; }',
      '  </style>',
      '</head>',
      '<body>',
      '  <div class="card">',
      '    <h1>Classic Load Balancer</h1>',
      '    <div class="subtitle">AWS CDK Example &middot; TypeScript &middot; aws-cdk-lib</div>',
      '    <div class="grid">',
      '      <div class="item"><div class="label">Instance ID</div><div class="value">\'$INSTANCE_ID\'</div></div>',
      '      <div class="item"><div class="label">Availability Zone</div><div class="value">\'$AZ\'</div></div>',
      '      <div class="item"><div class="label">Instance Type</div><div class="value">\'$INSTANCE_TYPE\'</div></div>',
      '      <div class="item"><div class="label">Architecture</div><div class="value">\'$ARCH\'</div></div>',
      '      <div class="item"><div class="label">AMI ID</div><div class="value">\'$AMI_ID\'</div></div>',
      '      <div class="item"><div class="label">Kernel</div><div class="value">\'$KERNEL\'</div></div>',
      '      <div class="item"><div class="label">Web Server</div><div class="value">\'$HTTPD_VERSION\'</div></div>',
      '      <div class="item"><div class="label">CDK Construct</div><div class="value">elb.LoadBalancer</div></div>',
      '    </div>',
      '    <div class="footer">Deployed with AWS CDK &middot; TypeScript &middot; Launch Template &middot; Amazon Linux 2023 (ARM64)</div>',
      '  </div>',
      '</body>',
      '</html>',
      'HTML',
      'systemctl start httpd',
    );

    const launchTemplate = new ec2.LaunchTemplate(this, 'LaunchTemplate', {
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.BURSTABLE4_GRAVITON, ec2.InstanceSize.MICRO),
      machineImage: ec2.MachineImage.latestAmazonLinux2023({
        cpuType: ec2.AmazonLinuxCpuType.ARM_64,
      }),
      requireImdsv2: true,
      userData,
      securityGroup: sg,
    });

    const asg = new autoscaling.AutoScalingGroup(this, 'ASG', {
      vpc,
      launchTemplate,
    });

    const lb = new elb.LoadBalancer(this, 'LB', {
      vpc,
      internetFacing: true,
      healthCheck: { port: 80 },
    });

    lb.addTarget(asg);
    lb.addListener({ externalPort: 80 });
  }
}

const app = new cdk.App();
new LoadBalancerStack(app, 'LoadBalancerStack');
app.synth();
