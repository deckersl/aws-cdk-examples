#!/bin/bash
yum install -y httpd
systemctl enable httpd
systemctl start httpd

TOKEN=$(curl -s -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 300")
META="curl -s -H \"X-aws-ec2-metadata-token: $TOKEN\" http://169.254.169.254/latest/meta-data"
IID=$(eval $META/instance-id)
AZ=$(eval $META/placement/availability-zone)
ITYPE=$(eval $META/instance-type)
ARCH=$(uname -m)
AMI=$(eval $META/ami-id)
KERN=$(uname -r)
HTTPD_V=$(httpd -v | head -1)

cat > /var/www/html/index.html <<EOF
<!DOCTYPE html><html><head><meta charset="utf-8"><title>Sandbox Info</title>
<style>
body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;background:#1a1a2e;color:#e0e0e0;font-family:system-ui}
.card{background:#16213e;border-radius:12px;padding:2rem 3rem;box-shadow:0 8px 32px rgba(0,0,0,.4);min-width:380px}
h1{color:#0f9ef7;margin:0 0 1.5rem;font-size:1.4rem;text-align:center}
.row{display:flex;justify-content:space-between;padding:.5rem 0;border-bottom:1px solid #1a1a2e}
.label{color:#8892b0}.value{color:#ccd6f6;font-family:monospace}
</style></head><body><div class="card"><h1>&#9729; Sandbox Instance</h1>
<div class="row"><span class="label">Instance ID</span><span class="value">$IID</span></div>
<div class="row"><span class="label">Availability Zone</span><span class="value">$AZ</span></div>
<div class="row"><span class="label">Instance Type</span><span class="value">$ITYPE</span></div>
<div class="row"><span class="label">Architecture</span><span class="value">$ARCH</span></div>
<div class="row"><span class="label">AMI ID</span><span class="value">$AMI</span></div>
<div class="row"><span class="label">Kernel</span><span class="value">$KERN</span></div>
<div class="row"><span class="label">Web Server</span><span class="value">$HTTPD_V</span></div>
<div class="row"><span class="label">CDK Construct</span><span class="value">AutoScalingGroup + ALB</span></div>
</div></body></html>
EOF
