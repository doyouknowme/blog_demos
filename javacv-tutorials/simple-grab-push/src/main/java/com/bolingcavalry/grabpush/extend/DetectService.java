package com.bolingcavalry.grabpush.extend;

import com.bolingcavalry.grabpush.Constants;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * @author willzhao
 * @version 1.0
 * @description 检测工具的通用接口
 * @date 2021/12/5 10:57
 */
public interface DetectService {

    /**
     * 根据传入的MAT构造相同尺寸的MAT，存放灰度图片用于以后的检测
     * @param src 原始图片的MAT对象
     * @return 相同尺寸的灰度图片的MAT对象
     */
    static Mat buildGrayImage(Mat src) {
        return new Mat(src.rows(), src.cols(), CV_8UC1);
    }

    /**
     * 检测图片，将检测结果用矩形标注在原始图片上
     * @param classifier 分类器
     * @param converter Frame和mat的转换器
     * @param rawFrame 原始视频帧
     * @param grabbedImage 原始视频帧对应的mat
     * @param grayImage 存放灰度图片的mat
     * @return 标注了识别结果的视频帧
     */
    static Frame detect(CascadeClassifier classifier,
                        OpenCVFrameConverter.ToMat converter,
                        Frame rawFrame,
                        Mat grabbedImage,
                        Mat grayImage) {

        // 当前图片转为灰度图片
        cvtColor(grabbedImage, grayImage, CV_BGR2GRAY);

        // 存放检测结果的容器
        RectVector objects = new RectVector();

        // 开始检测
        classifier.detectMultiScale(grayImage, objects);

        // 检测结果总数
        long total = objects.size();

        // 如果没有检测到结果，就用原始帧返回
        if (total<1) {
            return rawFrame;
        }

        // 如果有检测结果，就根据结果的数据构造矩形框，画在原图上
        for (long i = 0; i < total; i++) {
            Rect r = objects.get(i);
            int x = r.x(), y = r.y(), w = r.width(), h = r.height();
            rectangle(grabbedImage, new Point(x, y), new Point(x + w, y + h), Scalar.RED, 1, CV_AA, 0);
        }

        // 释放检测结果资源
        objects.close();

        // 将标注过的图片转为帧，返回
        return converter.convert(grabbedImage);
    }

    /**
     *
     * @param classifier 分类器
     * @param converter 转换工具
     * @param rawFrame 原始帧
     * @param grabbedImage 原始图片的Mat对象
     * @param grayImage 原始图片对应的灰度图片的Mat对象
     * @param basePath 图片的基本路径
     * @param maxFacePerFrame 一帧中最多的人脸数（假设等于2，那么一旦检测出的人脸数大于2，就认为本次检测不准确，放弃对当前帧的处理，返回原始对象，0表示不做限制）
     * @return
     */
    static Frame detectAndSave(CascadeClassifier classifier,
                              OpenCVFrameConverter.ToMat converter,
                              Frame rawFrame,
                              Mat grabbedImage,
                              Mat grayImage,
                              String basePath,
                              int maxFacePerFrame) {

        // 当前图片转为灰度图片
        cvtColor(grabbedImage, grayImage, CV_BGR2GRAY);

        // 存放检测结果的容器
        RectVector objects = new RectVector();

        // 开始检测
        classifier.detectMultiScale(grayImage, objects);

        // 检测结果总数
        long total = objects.size();

        // 如果没有检测到结果就提前返回
        if (total<1) {
            return rawFrame;
        }

        // 如果入参明确说明的当前帧的人脸数不超过1，
        // 那么一旦识别出的人脸数比1多，就证明识别有问题，放弃后续处理，直接返回原始帧，
        // 在一个人对着摄像头截图用于训练时适合传入1，因为此时检测的结果如果大于1，显然是检测有问题
        if (maxFacePerFrame>0 && total>maxFacePerFrame) {
            return rawFrame;
        }

        int saveNums = 0;

        Mat faceMat;

        Size size = new Size(Constants.RESIZE_WIDTH, Constants.RESIZE_HEIGHT);

        // 如果有检测结果，就根据结果的数据构造矩形框，画在原图上
        for (long i = 0; i < total; i++) {
            Rect r = objects.get(i);
            faceMat = new Mat(grayImage, r);

            resize(faceMat, faceMat, size);

            // 图片的保存位置
            String imagePath = basePath + (saveNums++) + "." + Constants.IMG_TYPE;

            // 保存图片
            imwrite(imagePath, faceMat);

            int x = r.x(), y = r.y(), w = r.width(), h = r.height();

            rectangle(grabbedImage, new Point(x, y), new Point(x + w, y + h), Scalar.RED, 1, CV_AA, 0);
        }

        // 释放检测结果资源
        objects.close();

        // 将标注过的图片转为帧，返回
        return converter.convert(grabbedImage);
    }

    /**
     * 检测图片，将检测结果用矩形标注在原始图片上
     * @param classifier 分类器
     * @param converter Frame和mat的转换器
     * @param rawFrame 原始视频帧
     * @param grabbedImage 原始视频帧对应的mat
     * @param grayImage 存放灰度图片的mat
     * @return 标注了识别结果的视频帧
     */
    static Frame detectAndRecoginze(CascadeClassifier classifier,
                        OpenCVFrameConverter.ToMat converter,
                        Frame rawFrame,
                        Mat grabbedImage,
                        Mat grayImage,
                        RecognizeService recognizeService) {

        // 当前图片转为灰度图片
        cvtColor(grabbedImage, grayImage, CV_BGR2GRAY);

        // 存放检测结果的容器
        RectVector objects = new RectVector();

        // 开始检测
        classifier.detectMultiScale(grayImage, objects);

        // 检测结果总数
        long total = objects.size();

        // 如果没有检测到结果，就用原始帧返回
        if (total<1) {
            return rawFrame;
        }

        PredictRlt predictRlt;
        int pos_x;
        int pos_y;
        String content;

        // 如果有检测结果，就根据结果的数据构造矩形框，画在原图上
        for (long i = 0; i < total; i++) {
            Rect r = objects.get(i);

            predictRlt = recognizeService.predict(new Mat(grayImage, r));

            if(predictRlt.getConfidence()>Constants.MAX_CONFIDENCE) {
                continue;
            }

            int x = r.x(), y = r.y(), w = r.width(), h = r.height();
            rectangle(grabbedImage, new Point(x, y), new Point(x + w, y + h), Scalar.RED, 1, CV_AA, 0);

            content = String.format("label : %d, confidence : %.4f", predictRlt.getLable(), predictRlt.getConfidence());

            pos_x = Math.max(r.tl().x()-10, 0);
            pos_y = Math.max(r.tl().y()-10, 0);

            putText(grabbedImage, content, new Point(pos_x, pos_y), FONT_HERSHEY_PLAIN, 1.0, new Scalar(0,255,0,2.0));
        }

        // 释放检测结果资源
        objects.close();

        // 将标注过的图片转为帧，返回
        return converter.convert(grabbedImage);
    }








    /**
     * 初始化操作，例如模型下载
     * @throws Exception
     */
    void init() throws Exception;

    /**
     * 得到原始帧，做识别，添加框选
     * @param frame
     * @return
     */
    Frame convert(Frame frame);

    /**
     * 释放资源
     */
    void releaseOutputResource();
}