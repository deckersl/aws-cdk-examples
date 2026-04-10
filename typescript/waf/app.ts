#!/usr/bin/env node

import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';

import { WafRegionalStack }   from './waf-regional';
import { WafCloudFrontStack } from './waf-cloudfront';

const app = new cdk.App();

new WafRegionalStack(app,   'WafRegionalStack',   { description: 'WAF Regional' });
new WafCloudFrontStack(app, 'WafCloudFrontStack', { description: 'WAF CloudFront' });

app.synth();
