import * as ec2 from 'aws-cdk-lib/aws-ec2';
import { Construct } from 'constructs';

export class VPCResources extends Construct {
  public sshSecurityGroup: ec2.SecurityGroup;
  public vpc: ec2.Vpc;

  constructor(scope: Construct, id: string) {
    super(scope, id);

    this.vpc = new ec2.Vpc(this, 'VPC', {
      natGateways: 0,
      subnetConfiguration: [
        {
          cidrMask: 24,
          name: 'ServerPublic',
          subnetType: ec2.SubnetType.PUBLIC,
          mapPublicIpOnLaunch: true,
        },
      ],
      maxAzs: 2,
    });

    this.sshSecurityGroup = new ec2.SecurityGroup(this, 'SSHSecurityGroup', {
      vpc: this.vpc,
      description: 'Security Group for SSH',
      allowAllOutbound: true,
    });

    this.sshSecurityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(22));
  }
}
