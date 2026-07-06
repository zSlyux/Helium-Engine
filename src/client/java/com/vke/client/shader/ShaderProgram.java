package com.vke.client.shader;

public record ShaderProgram(String name, byte[] vertexSpirv, byte[] fragmentSpirv) {
}
