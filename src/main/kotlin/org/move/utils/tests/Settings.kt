package org.move.utils.tests

import junit.framework.TestCase
import org.move.cli.settings.ProjectType

/** Tries to find the specified annotation on the current test method and then on the current class */
inline fun <reified T : Annotation> TestCase.findAnnotationInstance(): T? =
    javaClass.getMethod(name).getAnnotation(T::class.java) ?: javaClass.getAnnotation(T::class.java)

inline fun <reified T : Annotation> MvProjectTestBase.findAnnotationInstance(): T? =
    javaClass.getMethod(name).getAnnotation(T::class.java) ?: javaClass.getAnnotation(T::class.java)

annotation class SettingsProjectType(val type: ProjectType)
