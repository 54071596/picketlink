/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.picketlink.idm.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.picketlink.idm.model.AttributedType;

/**
 * General purpose Util
 *
 * @author anil saldhana
 * @since Sep 13, 2012
 *
 */
public class IDMUtil {

    /**
     * <p>Converts the given array into a {@link Set}.</p>
     *
     * @param values
     * @param <P>
     * @return
     */
    public static <P> Set<P> toSet(P[] values) {
        return new HashSet<P>(Arrays.asList(values));
    }

    /**
     * <p>Determines if a specific type is supported considering its class hierarchy.</p>
     * <p>A score is returned to determine the support level for the type.</p>
     *
     * @param type
     * @param supportedTypes
     * @param unsupportedTypes
     * @param <P>
     * @return -1 the type is not supported. Otherwise the support score for this type.
     */
    public static <P extends Class<?>> int isTypeSupported(P type, Set<P> supportedTypes, Set<P> unsupportedTypes) {
        int score = -1;

        for (P cls : supportedTypes) {
            int clsScore = calcScore(type, cls);
            if (clsScore > score && supportedTypes.contains(cls)) {
                score = clsScore;
            }
        }

        for (Class<?> cls : unsupportedTypes) {
            if (cls.isAssignableFrom(type) && unsupportedTypes.contains(cls)) {
                score = -1;
                break;
            }
        }
        return score;
    }

    private static int calcScore(Class<?> type, Class<?> targetClass) {
        if (type.equals(targetClass)) {
            return 0;
        } else if (targetClass.isAssignableFrom(type)) {
            int score = 0;

            Class<?> cls = type.getSuperclass();
            while (!cls.equals(Object.class)) {
                if (targetClass.isAssignableFrom(cls)) {
                    score++;
                } else {
                    break;
                }
                cls = cls.getSuperclass();
            }
            return score;
        } else {
            return -1;
        }
    }
}