from aws_cdk import App, Environment

from waf_regional   import WafRegionalStack
from waf_cloudfront import WafCloudFrontStack

app = App()

WafRegionalStack(app,   "WafRegionalStack")
WafCloudFrontStack(app, "WafCloudFrontStack", env=Environment(region="us-east-1"))

app.synth()
