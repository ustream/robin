/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
/**
 * Annotation for tests that can not run from multiple threads in the same time.
 */
package com.robin.testcase.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author ChaotX
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Sequential
{
    /**
     * Set this to true if the method can not run while any other sequential
     * method is running.
     */
    boolean globally() default false;

    /**
     * Array of classes to suspend method execution till any other sequential
     * test is running that lists any of these classes.
     */
    Class<?>[] groupClasses() default {};
}
