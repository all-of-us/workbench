

variable "notification_channel_info" {
  description = <<EOF
I want to use an  array  of objects,  but  as of v0.13, only
sets of strings or  single objects are supported.l Hence I'm
passing dummy keys as  the outermost entries, named _0, _1, etc.
TODO(jaycarlton) move to collection of objects wen that's an  actual thing.
{
    _0 = {
      display_name = "An email channel"
      type         = "email" # email or
      labels = {
        email = ""
      }
    },
    _1 = {
      display_name = "Slack Random Channel"
      type         = "slack"
      labels = {
        channel = "#random"
      }
    }
EOF

  type = map
}
