package software.amazon.awscdk.examples;

import java.util.UUID;
import software.constructs.Construct;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.SingletonFunction;

/** Lambda Cron CDK example for Java! */
class LambdaCronStack extends Stack {
  public LambdaCronStack(final Construct parent, final String name) {
    super(parent, name);

    SingletonFunction lambdaFunction =
        SingletonFunction.Builder.create(this, "cdk-lambda-cron")
            .description("Scheduled Lambda — CDK Java Example")
            .code(Code.fromInline(
                "import json, sys, platform, os\n" +
                "def main(event, context):\n" +
                "    info = {\n" +
                "        'project': 'lambda-cron',\n" +
                "        'language': 'Java (CDK) / Python (runtime)',\n" +
                "        'runtime': f'Python {sys.version}',\n" +
                "        'platform': platform.platform(),\n" +
                "        'architecture': platform.machine(),\n" +
                "        'handler': context.function_name,\n" +
                "        'region': os.environ.get('AWS_REGION', 'unknown'),\n" +
                "        'cdk_construct': 'SingletonFunction + Rule',\n" +
                "        'schedule': 'cron(0 18 ? * MON-FRI *)',\n" +
                "    }\n" +
                "    print(json.dumps(info, indent=2))\n" +
                "    return info\n"))
            .handler("index.main")
            .timeout(Duration.seconds(300))
            .runtime(Runtime.PYTHON_3_13)
            .uuid(UUID.randomUUID().toString())
            .build();

    Rule rule =
        Rule.Builder.create(this, "cdk-lambda-cron-rule")
            .description("Run every day at 6PM UTC")
            .schedule(Schedule.expression("cron(0 18 ? * MON-FRI *)"))
            .build();

    rule.addTarget(new LambdaFunction(lambdaFunction));
  }
}
