package eu.cybershu.pocketstats.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.validator.GenericValidator;

public class CheckDateValidator implements ConstraintValidator<CheckDateFormat, String> {
    private String pattern;

    @Override
    public void initialize(CheckDateFormat constraintAnnotation) {
        this.pattern = constraintAnnotation.pattern();
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean isValid(String object, ConstraintValidatorContext constraintContext) {
        if (object == null) {
            return true;
        }

        return GenericValidator.isDate(object, this.pattern, true);
    }
}