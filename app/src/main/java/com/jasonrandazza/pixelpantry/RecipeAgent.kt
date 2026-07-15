package com.jasonrandazza.pixelpantry

import ai.koog.agents.AIAgent
import ai.koog.llm.clients.AnthropicLLMClient
import ai.koog.llm.models.AnthropicModels
import ai.koog.prompt.MultiLLMPromptExecutor

class RecipeAgent(apiKey: String) {

    private val agent = AIAgent(
        promptExecutor = MultiLLMPromptExecutor(AnthropicLLMClient(apiKey)),
        llmModel = AnthropicModels.Sonnet_3_7
    )

    suspend fun findRecipe(ingredients: String): String {
        return agent.run(
            "You are a helpful recipe assistant. Given these ingredients: $ingredients, " +
            "suggest a recipe with step-by-step instructions."
        ) ?: "No recipe found."
    }
}
