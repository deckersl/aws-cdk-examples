import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { VPCResources } from './constructs/vpc';
import { ServerResources } from './constructs/server';
import { EC2ExampleProps, envValidator } from './utils/env-validator';

export interface EC2StackProps extends cdk.StackProps, EC2ExampleProps {}

export class EC2Stack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: EC2StackProps) {
    super(scope, id, props);

    envValidator(props);

    const vpcResources = new VPCResources(this, 'VPC');

    const serverResources = new ServerResources(this, 'EC2', {
      vpc: vpcResources.vpc,
      sshSecurityGroup: vpcResources.sshSecurityGroup,
      logLevel: props.logLevel,
      sshPubKey: props.sshPubKey,
      cpuType: props.cpuType,
      instanceSize: props.instanceSize.toLowerCase(),
    });

    new cdk.CfnOutput(this, 'ssmCommand', {
      value: `aws ssm start-session --target ${serverResources.instance.instanceId}`,
    });

    new cdk.CfnOutput(this, 'sshCommand', {
      value: `ssh ec2-user@${serverResources.instance.instancePublicDnsName}`,
    });
  }
}
