package com.dreamfish.sea.oldbook.util;

import com.dreamfish.sea.oldbook.entity.User;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/13 14:51
 */
public class MyConstraintValidator implements ConstraintValidator<PasswordMatches, User> {
    @Override
    public boolean isValid(User value, ConstraintValidatorContext context) {
        return value.getPassword().equals(value.getMatchingPassword());
    }

}
