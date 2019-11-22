package com.better.alarm.configuration

import com.better.alarm.logger.LoggerFactory
import org.koin.core.context.GlobalContext.get
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier

/**
 * globalInject lazily given dependency for Android koincomponent
 * @param qualifier - bean qualifier / optional
 * @param scope
 * @param parameters - injection parameters
 */
inline fun <reified T : Any> globalInject(
        qualifier: Qualifier? = null,
        noinline parameters: ParametersDefinition? = null
) = lazy { get().koin.rootScope.get<T>(qualifier, parameters) }

inline fun globalLogger(tag: String) = lazy { get().koin.rootScope.get<LoggerFactory>().createLogger(tag) }

/**
 * globalInject lazily given dependency for Android koincomponent
 * @param qualifier - bean qualifier / optional
 * @param scope
 * @param parameters - injection parameters
 */
@Deprecated("Java", ReplaceWith("globalInject()"))
fun <T : Any> globalInject(clazz: Class<T>) = lazy { get().koin.rootScope.get<T>(clazz = clazz) }