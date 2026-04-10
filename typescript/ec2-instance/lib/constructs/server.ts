import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as s3deploy from 'aws-cdk-lib/aws-s3-deployment';
import { Construct } from 'constructs';

interface ServerProps {
  vpc: ec2.Vpc;
  sshSecurityGroup: ec2.SecurityGroup;
  logLevel: string;
  sshPubKey: string;
  cpuType: string;
  instanceSize: string;
}

export class ServerResources extends Construct {
  public instance: ec2.Instance;

  constructor(scope: Construct, id: string, props: ServerProps) {
    super(scope, id);

    const assetBucket = new s3.Bucket(this, 'assetBucket', {
      publicReadAccess: false,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
      objectOwnership: s3.ObjectOwnership.BUCKET_OWNER_PREFERRED,
      autoDeleteObjects: true,
    });

    new s3deploy.BucketDeployment(this, 'assetBucketDeployment', {
      sources: [s3deploy.Source.asset('lib/resources/server/assets')],
      destinationBucket: assetBucket,
      retainOnDelete: false,
      exclude: ['**/node_modules/**', '**/dist/**'],
      memoryLimit: 512,
    });

    const serverRole = new iam.Role(this, 'serverEc2Role', {
      assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
      inlinePolicies: {
        ['RetentionPolicy']: new iam.PolicyDocument({
          statements: [
            new iam.PolicyStatement({
              resources: ['*'],
              actions: ['logs:PutRetentionPolicy'],
            }),
          ],
        }),
      },
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore'),
        iam.ManagedPolicy.fromAwsManagedPolicyName('CloudWatchAgentServerPolicy'),
      ],
    });

    assetBucket.grantReadWrite(serverRole);

    const cpuType = props.cpuType === 'ARM64'
      ? ec2.AmazonLinuxCpuType.ARM_64
      : ec2.AmazonLinuxCpuType.X86_64;

    const instanceClass = props.cpuType === 'ARM64'
      ? ec2.InstanceClass.M7G
      : ec2.InstanceClass.M5;

    const sizeMap: Record<string, ec2.InstanceSize> = {
      large: ec2.InstanceSize.LARGE,
      xlarge: ec2.InstanceSize.XLARGE,
      xlarge2: ec2.InstanceSize.XLARGE2,
      xlarge4: ec2.InstanceSize.XLARGE4,
    };
    const instanceSize = sizeMap[props.instanceSize] ?? ec2.InstanceSize.LARGE;

    const userData = ec2.UserData.forLinux();
    userData.addCommands(
      'yum update -y',
      'yum install -y amazon-cloudwatch-agent python3-pip zip unzip docker httpd',
      'systemctl enable docker && systemctl start docker',
      'systemctl enable httpd && systemctl start httpd',
      // IMDSv2 info page — fetches instance metadata and renders styled HTML
      'TOKEN=$(curl -s -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")',
      'MH="X-aws-ec2-metadata-token: $TOKEN"',
      'INST_ID=$(curl -s -H "$MH" http://169.254.169.254/latest/meta-data/instance-id)',
      'AZ=$(curl -s -H "$MH" http://169.254.169.254/latest/meta-data/placement/availability-zone)',
      'INST_TYPE=$(curl -s -H "$MH" http://169.254.169.254/latest/meta-data/instance-type)',
      'ARCH=$(uname -m)',
      'AMI_ID=$(curl -s -H "$MH" http://169.254.169.254/latest/meta-data/ami-id)',
      'KERNEL=$(uname -r)',
      'HTTPD_VER=$(httpd -v | head -1 | awk \'{print $3}\')',
      'cat > /var/www/html/index.html << EOF\n'
      + '<!DOCTYPE html><html><head><meta charset="utf-8"><title>EC2 Instance Info</title>\n'
      + '<style>*{margin:0;padding:0;box-sizing:border-box}body{background:#1a1a2e;color:#e0e0e0;font-family:system-ui,sans-serif;display:flex;justify-content:center;align-items:center;min-height:100vh}.card{background:#16213e;border-radius:12px;padding:2rem;max-width:480px;width:90%;box-shadow:0 8px 32px rgba(0,0,0,.4)}h1{color:#0f9;font-size:1.4rem;margin-bottom:1.2rem;text-align:center}.row{display:flex;justify-content:space-between;padding:.5rem 0;border-bottom:1px solid #1a1a2e}.label{color:#888}.value{color:#0cf;font-family:monospace}</style></head><body><div class="card"><h1>&#x1F4BB; EC2 Instance Info</h1>\n'
      + '<div class="row"><span class="label">Instance ID</span><span class="value">$INST_ID</span></div>\n'
      + '<div class="row"><span class="label">Availability Zone</span><span class="value">$AZ</span></div>\n'
      + '<div class="row"><span class="label">Instance Type</span><span class="value">$INST_TYPE</span></div>\n'
      + '<div class="row"><span class="label">Architecture</span><span class="value">$ARCH</span></div>\n'
      + '<div class="row"><span class="label">AMI ID</span><span class="value">$AMI_ID</span></div>\n'
      + '<div class="row"><span class="label">Kernel</span><span class="value">$KERNEL</span></div>\n'
      + '<div class="row"><span class="label">Web Server</span><span class="value">$HTTPD_VER</span></div>\n'
      + '<div class="row"><span class="label">CDK Construct</span><span class="value">aws-cdk-lib/aws-ec2.Instance</span></div>\n'
      + '</div></body></html>\nEOF',
      'mkdir -p /home/ec2-user/sample',
      'aws s3 cp s3://' + assetBucket.bucketName + '/sample /home/ec2-user/sample --recursive',
    );

    const ec2InstanceSecurityGroup = new ec2.SecurityGroup(this, 'ec2InstanceSecurityGroup', {
      vpc: props.vpc,
      allowAllOutbound: true,
    });

    // Allow HTTP inbound for the info page
    ec2InstanceSecurityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(80), 'HTTP');

    this.instance = new ec2.Instance(this, 'Instance', {
      vpc: props.vpc,
      instanceType: ec2.InstanceType.of(instanceClass, instanceSize),
      machineImage: ec2.MachineImage.latestAmazonLinux2023({
        cachedInContext: false,
        cpuType: cpuType,
      }),
      requireImdsv2: true,
      userData: userData,
      securityGroup: ec2InstanceSecurityGroup,
      init: ec2.CloudFormationInit.fromConfigSets({
        configSets: { default: ['config'] },
        configs: {
          config: new ec2.InitConfig([
            ec2.InitFile.fromObject('/etc/config.json', {
              STACK_ID: cdk.Stack.of(this).artifactId,
            }),
            ec2.InitFile.fromFileInline(
              '/tmp/amazon-cloudwatch-agent.json',
              './lib/resources/server/config/amazon-cloudwatch-agent.json',
            ),
            ec2.InitFile.fromFileInline(
              '/etc/config.sh',
              'lib/resources/server/config/config.sh',
            ),
            ec2.InitFile.fromString(
              '/home/ec2-user/.ssh/authorized_keys',
              props.sshPubKey + '\n',
            ),
            ec2.InitCommand.shellCommand('chmod +x /etc/config.sh'),
            ec2.InitCommand.shellCommand('/etc/config.sh'),
          ]),
        },
      }),
      initOptions: {
        timeout: cdk.Duration.minutes(10),
        includeUrl: true,
        includeRole: true,
        printLog: true,
      },
      role: serverRole,
    });

    this.instance.addSecurityGroup(props.sshSecurityGroup);
  }
}
