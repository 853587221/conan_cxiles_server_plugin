import os
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import io

try:
    from nudenet import NudeDetector
except ImportError:
    print("nudenet 未安装，正在安装...")
    os.system(f"{sys.executable} -m pip install nudenet --no-cache-dir")
    from nudenet import NudeDetector

class NSFWChecker:
    def __init__(self):
        try:
            self.detector = NudeDetector()
            print("[NSFW] NudeDetector 初始化成功")
        except Exception as e:
            print(f"[NSFW] 初始化失败: {e}")
            self.detector = None

    def check_image(self, image_path):
        if not self.detector:
            print("[NSFW] 检测器未初始化，跳过检查")
            return {'is_nsfw': False, 'violations': []}

        try:
            print(f"[NSFW] 开始检查图片: {image_path}")
            results = self.detector.detect(str(image_path))
            print(f"[NSFW] 原始检测结果: {results}")
            
            violations = []
            is_nsfw = False

            for result in results:
                label = result.get('class', '')
                score = result.get('score', 0)
                
                print(f"[NSFW] 检测到: {label}, 置信度: {score:.2f}, 完整结果: {result}")

                if score < 0.3:
                    continue

                if label == 'FEMALE_GENITALIA' and score > 0.5:
                    violations.append({'label': '女性生殖器', 'score': score})
                    is_nsfw = True
                elif label == 'FEMALE_GENITALIA_EXPOSED' and score > 0.5:
                    violations.append({'label': '女性生殖器暴露', 'score': score})
                    is_nsfw = True
                elif label == 'ANUS_EXPOSED' and score > 0.5:
                    violations.append({'label': '肛门暴露', 'score': score})
                    is_nsfw = True
                elif label == 'BUTTOCKS_EXPOSED' and score > 0.6:
                    violations.append({'label': '臀部暴露', 'score': score})
                    is_nsfw = True
                elif label == 'FEMALE_BREAST_EXPOSED' and score > 0.5:
                    violations.append({'label': '女性胸部暴露', 'score': score})
                    is_nsfw = True

            print(f"[NSFW] 检查完成: is_nsfw={is_nsfw}, violations={len(violations)}")
            return {'is_nsfw': is_nsfw, 'violations': violations}
            
        except Exception as e:
            print(f"[NSFW] 检查失败: {e}")
            import traceback
            traceback.print_exc()
            return {'is_nsfw': False, 'violations': []}

    def blur_and_add_warning(self, image_path, output_path=None):
        try:
            img = Image.open(image_path)
            original_format = img.format if img.format else 'PNG'
            
            width, height = img.size
            
            blurred = img.filter(ImageFilter.GaussianBlur(radius=30))
            
            draw = ImageDraw.Draw(blurred)
            
            font_size = max(20, min(width, height) // 20)
            
            font = None
            chinese_font_paths = [
                "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
                "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Bold.ttc",
                "/System/Library/Fonts/PingFang.ttc",
                "/System/Library/Fonts/STHeiti Light.ttc",
                "C:/Windows/Fonts/msyh.ttc",
                "C:/Windows/Fonts/simhei.ttf",
            ]
            
            for font_path in chinese_font_paths:
                try:
                    if os.path.exists(font_path):
                        font = ImageFont.truetype(font_path, font_size)
                        print(f"[NSFW] 使用字体: {font_path}")
                        break
                except:
                    continue
            
            if not font:
                try:
                    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", font_size)
                except:
                    try:
                        font = ImageFont.truetype("arial.ttf", font_size)
                    except:
                        font = ImageFont.load_default()

            text = "图像涉嫌违规"
            
            bbox = draw.textbbox((0, 0), text, font=font)
            text_width = bbox[2] - bbox[0]
            text_height = bbox[3] - bbox[1]

            x = (width - text_width) // 2
            y = (height - text_height) // 2

            padding = 20
            rect_x1 = x - padding
            rect_y1 = y - padding
            rect_x2 = x + text_width + padding
            rect_y2 = y + text_height + padding
            
            draw.rectangle(
                [rect_x1, rect_y1, rect_x2, rect_y2],
                fill=(0, 0, 0, 200)
            )
            
            draw.text((x, y), text, fill=(255, 255, 255), font=font)

            save_path = output_path if output_path else image_path
            
            if original_format == 'WEBP':
                blurred.save(save_path, format='WEBP', quality=85)
            elif original_format == 'JPEG':
                blurred.save(save_path, format='JPEG', quality=85)
            elif original_format == 'GIF':
                blurred.save(save_path, format='PNG')
            elif original_format == 'BMP':
                blurred.save(save_path, format='PNG')
            else:
                blurred.save(save_path, format='PNG')
            
            if output_path:
                print(f"[NSFW] 已保存处理后的图片: {output_path}")
            else:
                print(f"[NSFW] 已覆盖原图片: {image_path}")

            return True
        except Exception as e:
            print(f"[NSFW] 模糊处理失败: {e}")
            import traceback
            traceback.print_exc()
            return False

def check_and_process_image(image_path, output_path=None):
    checker = NSFWChecker()
    result = checker.check_image(image_path)
    
    if result['is_nsfw']:
        print(f"[NSFW] 图片包含违规内容: {result['violations']}")
        success = checker.blur_and_add_warning(image_path, output_path)
        return {'success': success, 'is_nsfw': True, 'violations': result['violations']}
    else:
        print("[NSFW] 图片检查通过")
        return {'success': True, 'is_nsfw': False, 'violations': []}

if __name__ == '__main__':
    import sys
    if len(sys.argv) > 1:
        image_path = sys.argv[1]
        result = check_and_process_image(image_path)
        print(f"检查结果: {result}")
    else:
        print("使用方法: python check_nsfw.py <图片路径>")
