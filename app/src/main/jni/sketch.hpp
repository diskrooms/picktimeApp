//
// Created by jsb-hdp-0 on 2017/8/26.
//

#ifndef PICKTIME_SKETCH_H
#define PICKTIME_SKETCH_H
#include <vector>
#include <opencv2/core/core.hpp>
namespace picktime {
    using cv::Mat;
    class PencilDrawing {
        private:
            float weight_1;
            float weight_2;
            float weight_3;
            float delta_b;
            float u_a;
            float u_b;
            float u_d;
            float delta_d;
            std::vector<double> distribution;
        public:
            PencilDrawing();
            /**
             * @param image 输入图像
             * @param pattern 铅笔模式
             * @param pencilDrawing 绘制出的铅笔画
             */
            void operator()(const Mat& image, const Mat& pattern, Mat& pencilDrawing) const;
        private:
            // 计算梯度
            void gradient(const Mat& img, Mat& gradientImg) const;
            // 生成笔画
            void generateStroke(const Mat& gradientImg, Mat& pencilStroke) const;
            // 计算分布率
            void computeDistribution();
            // 生成明暗图
            void generateToneMap(const Mat& img, Mat& toneMap) const;
            // 生成铅笔画纹理
            void generatePencilTexture(const Mat& toneMap, const Mat& pattern, Mat& pencilTexture) const;
    };
}	// namespace picktime

#endif //PICKTIME_SKETCH_H
