package org.pmiops.workbench.db.model

import com.google.common.truth.Truth.assertThat

import com.google.common.collect.BiMap
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import org.junit.Test

class StorageEnumsTest {
    @Test
    @Throws(Exception::class)
    fun noMissingMapEntries() {
        for (f in StorageEnums::class.java.declaredFields) {
            if (f.type != BiMap<*, *>::class.java) {
                continue
            }

            val enumClass = (f.annotatedType.type as ParameterizedType).actualTypeArguments[0] as Class<*>

            var enumToShort: Method? = null
            var shortToEnum: Method? = null
            for (m in StorageEnums::class.java.declaredMethods) {
                if (m.parameterTypes[0] == enumClass) {
                    enumToShort = m
                }

                if (m.returnType == enumClass) {
                    shortToEnum = m
                }
            }

            for (e in enumClass.enumConstants) {
                val shortVal = enumToShort!!.invoke(null, e) as Short

                assertThat(shortVal).named(enumClass.name + ":" + e.toString()).isNotNull()
                assertThat(shortToEnum!!.invoke(null, shortVal)).isEqualTo(e)
            }
        }
    }
}
