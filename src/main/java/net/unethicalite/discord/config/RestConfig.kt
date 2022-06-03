package net.unethicalite.discord.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import net.unethicalite.dto.exception.*
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.ResponseErrorHandler

@Configuration
class RestConfig : ResponseErrorHandler {
    private val objectMapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    @Bean
    fun restTemplate() = RestTemplateBuilder().errorHandler(this).build()

    override fun hasError(response: ClientHttpResponse) = response.statusCode.series() == HttpStatus.Series.CLIENT_ERROR
            || response.statusCode.series() == HttpStatus.Series.SERVER_ERROR

    override fun handleError(response: ClientHttpResponse) {
        val dto = objectMapper.readValue(response.body, ExceptionDto::class.java)
        val message = dto.message ?: "Backend error."

        when (response.statusCode) {
            HttpStatus.NOT_FOUND -> throw NotFoundException(message)
            HttpStatus.UNAUTHORIZED -> throw UnauthorizedException(message)
            HttpStatus.FORBIDDEN -> throw AuthenticationException(message)
            HttpStatus.BAD_REQUEST -> throw BadRequestException(message)

            else -> throw BackendException("Request error.")
        }
    }
}