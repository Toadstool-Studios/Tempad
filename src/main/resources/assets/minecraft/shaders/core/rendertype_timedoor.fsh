#version 150

const float pixelsPerUnit = 16.0;

in vec4 color;
in vec2 texCoord0;
in vec2 texCoord2;
in vec2 uv;
in vec2 uv2;

out vec4 fragColor;

void main() {
    float pixelWidth = (1.0 / 16.0) / (1 + ((uv.x - uv2.x) / uv2.x)); //pixel width
    float pixelHeight = (1.0 / 16.0) / (1 + ((uv.y - uv2.y) / uv2.y)); //pixel height
    vec2 pixelatedUV = vec2(uv2.x - mod(uv2.x, pixelWidth) + pixelWidth / 2, uv2.y - mod(uv2.y, pixelHeight) + pixelHeight / 2);

    float x = 2 * pixelatedUV.x - 1;
    float y = 2 * pixelatedUV.y - 1;
    float alpha = x * x / 3 + y * y / 3;
    fragColor = vec4(color.rgb, alpha - mod(alpha, 0.07) + 0.1);
}
