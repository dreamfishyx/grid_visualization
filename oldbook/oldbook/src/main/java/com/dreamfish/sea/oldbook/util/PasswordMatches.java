package com.dreamfish.sea.oldbook.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MyConstraintValidator.class)//使用MyConstraintValidator类的isValid方法来验证
@Target({ElementType.TYPE}) //类使用
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordMatches {
    String message() default "两次密码不一致";//错误信息

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
