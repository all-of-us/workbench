require_relative "../../aou-utils/utils/common"

YES_RESPONSES = ['roger', 'affirmative', 'yes', 'yep', 'positively', 'aye', 'definitely']

# Get user confirmation
def get_user_confirmation(message)
  yes_response = YES_RESPONSES.sample

  while true
    Common.new.status("#{message} [#{yes_response}/N]")
    answer = STDIN.gets.chomp
    if answer.downcase == yes_response
      return
    elsif answer.downcase == 'n'
      raise RuntimeError.new("Operation cancelled by user.")
    end
    # Try again, not an unambiguous yes or no
  end
end
