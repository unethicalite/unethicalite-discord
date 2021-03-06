package net.unethicalite.discord.service

import net.unethicalite.dto.exception.BackendException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class RestService(
    private val restTemplate: RestTemplate,
    @Value("\${backend.url}") private val baseUrl: String
) {

    fun <T> get(url: String, receivedType: Class<T>): T {
        return restTemplate.getForObject("$baseUrl$url", receivedType) ?: throw BackendException("Request failed.")
    }

    fun <T> post(url: String, body: Any?, receivedType: Class<T>): T {
        return restTemplate.postForObject("$baseUrl$url", body, receivedType) ?: throw BackendException("Request failed.")
    }

    fun put(url: String, body: Any?) = restTemplate.put("$baseUrl$url", body)

    fun delete(url: String) = restTemplate.delete("$baseUrl$url")
}