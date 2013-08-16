/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
/**
 * Annotation for tests using more than one device.
 */
package com.robin.testcase.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author ChaotX
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MultiDevice
{
    /**
     * Indicates the maximum number of devices the test use.
     */
    int maxDeviceUsed() default 2;
}
