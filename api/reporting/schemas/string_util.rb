def to_camel_case(snake_case, capitalize_initial)
  result =  snake_case.split('_').collect(&:capitalize).join
  unless capitalize_initial
    result[0] = result[0].downcase
  end
  result
end
