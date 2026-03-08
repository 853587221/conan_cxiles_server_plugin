from PIL import Image, ImageDraw

img = Image.new('RGB', (800, 600), color='white')
draw = ImageDraw.Draw(img)

draw.rectangle([100, 100, 700, 500], fill='pink')

output_path = "/www/wwwroot/LFZKN/RconClient/shop/test_webp_image.webp"
img.save(output_path, format='WEBP', quality=85)
print(f"WebP 测试图片已创建: {output_path}")
