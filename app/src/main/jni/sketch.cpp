#include <cmath>
#include <algorithm>
#include <vector>
#include <Eigen/Core>
#include <Eigen/IterativeLinearSolvers>
#include <opencv2/opencv.hpp>
#include <opencv2/core/eigen.hpp>
#include "sketch.hpp"

namespace picktime {
    PencilDrawing::PencilDrawing() :
        weight_1(11), weight_2(37), weight_3(52),
        delta_b(9),
        u_a(105), u_b(225),
        u_d(90), delta_d(11),
        distribution(256, 0.0) {
            this->computeDistribution();
        }

    static void cvtSingleChannelImg(const Mat& image_, Mat& singleChaImg,std::vector<Mat>& tmp_ = std::vector<Mat>()) {
        Mat image = image_.clone();
        if (image.cols > 511 && image.rows > 511)
            cv::pyrDown(image, image, cv::Size(image.cols / 2, image.rows / 2));

        if (image.channels() == 1)
            singleChaImg = image;
        else if (image.channels() == 3) {
            cv::cvtColor(image, singleChaImg, CV_BGR2YUV);
            std::vector<Mat> tmp(3, Mat());
            cv::split(singleChaImg, tmp);
            singleChaImg = tmp[0];
            if (tmp_.size() != 0){
                tmp_ = tmp;
            }
        } else {
            return;
        }
    }

    /**
     * @param image 需是BGR图或灰度图
     * @param pattern  需是BGR图或灰度图
     */
    void PencilDrawing::operator()(const Mat& image, const Mat& pattern, Mat& pencilDrawing) const {
        CV_Assert(!image.empty() && !pattern.empty());
        CV_Assert(image.rows > 59 && image.cols > 59);

        Mat img, style;
        std::vector<Mat> eachChannel(3, Mat());
        cvtSingleChannelImg(image, img, eachChannel);
        cvtSingleChannelImg(pattern, style);
        cv::resize(style, style, cv::Size(img.cols, img.rows));

        Mat gradientImg;	// 梯度图
        this->gradient(img, gradientImg);

        Mat pencilStroke;	// 线条笔画
        this->generateStroke(gradientImg, pencilStroke);

        Mat toneMap;		// 明暗图
        this->generateToneMap(img, toneMap);

        Mat pencilTexture;	// 铅笔画纹理
        this->generatePencilTexture(toneMap, style, pencilTexture);

        pencilDrawing = pencilStroke.mul(pencilTexture);
        pencilDrawing.convertTo(pencilDrawing, CV_8UC1);
        if (image.channels() == 3) {
            eachChannel[0] = pencilDrawing;
            cv::merge(eachChannel, pencilDrawing);
            cv::cvtColor(pencilDrawing, pencilDrawing, CV_YUV2BGR);
        }
    }

    /**
     * @param img 单通道图像
     * @param gradientImg 类型为CV_32FC1
     */
    void PencilDrawing::gradient(const Mat& img, Mat& gradientImg) const{
        Mat kernel_x(1, 2, CV_32FC1);
        Mat kernel_y(2, 1, CV_32FC1);
        kernel_x.at<float>(0, 0) = kernel_y.at<float>(0, 0) = -1;
        kernel_x.at<float>(0, 1) = kernel_y.at<float>(1, 0) = 1;
        Mat img_x, img_y;
        cv::filter2D(img, img_x, CV_32FC1, kernel_x);
        cv::filter2D(img, img_y, CV_32FC1, kernel_y);
        cv::pow(img_x, 2, img_x);
        cv::pow(img_y, 2, img_y);
        gradientImg = img_x + img_y;
        cv::pow(gradientImg, 0.5, gradientImg);
    }


    // return C_i(直接在G_i上修改得到C_i), generateStoke的辅助函数
    static std::vector<Mat>& getResponse(const Mat& gradientImg, std::vector<Mat>& G_i) {
        int maxIdx = 0;
        const int rows = G_i[0].rows;
        const int cols = G_i[0].cols;
        const int size = G_i.size();
        const float* gradientData = (float*)gradientImg.data;
        float** data = new float*[size];
        for (int i = 0; i < size; ++i)
            data[i] = (float*)G_i[i].data;

        int tmp;
        for (int r = 0; r < rows; ++r) {
            for (int c = 0; c < cols; ++c) {
                maxIdx = 0;
                tmp = r * cols + c;
                data[maxIdx][tmp] = gradientData[tmp];
                for (int i = 0; i < size; ++i) {
                    if (data[i][tmp] > data[maxIdx][tmp]) {
                        data[maxIdx][tmp] = 0;
                        maxIdx = i;
                        data[i][tmp] = gradientData[tmp];
                    } else {
                        data[i][tmp] = 0;
                    }
                }
            }
        }

        return G_i;
    }


    /**
     * @param gradientImg 类型为CV_32FC1
     * @param pencilStroke 类型为CV_32FC1
     */
    void PencilDrawing::generateStroke(const Mat& gradientImg, Mat& pencilStroke) const {
        int kernel_len = gradientImg.cols < gradientImg.rows ? gradientImg.cols : gradientImg.rows;
        kernel_len = (kernel_len / 60) * 2 + 1;
        Mat kernel_hori(1, kernel_len, CV_32FC1, cv::Scalar(0)); // 0 or 180
        Mat kernel_veri(kernel_len, 1, CV_32FC1, cv::Scalar(0)); // 90 or -90
        Mat kernel_diag(kernel_len, kernel_len, CV_32FC1, cv::Scalar(0)); // -135, -45, 45, 135

        kernel_hori.colRange(kernel_len / 2, kernel_len) = cv::Scalar(1);	// 0
        kernel_veri.rowRange(kernel_len / 2, kernel_len) = cv::Scalar(1);	// 90
        for (int i = 0; i < kernel_len / 2; ++i)	// -135
            kernel_diag.at<float>(i, i) = 1;

        // 0, 45, 90, 135, 180, -135, -90, -45
        std::vector<Mat> G_i(8, Mat());
        cv::filter2D(gradientImg, G_i[0], CV_32FC1, kernel_hori);	// 0
        cv::flip(kernel_hori, kernel_hori, 1);
        cv::filter2D(gradientImg, G_i[4], CV_32FC1, kernel_hori);	// 180
        cv::filter2D(gradientImg, G_i[2], CV_32FC1, kernel_veri);	// 90
        cv::flip(kernel_veri, kernel_veri, 0);
        cv::filter2D(gradientImg, G_i[6], CV_32FC1, kernel_veri);	// -90
        cv::filter2D(gradientImg, G_i[5], CV_32FC1, kernel_diag);	// -135
        cv::flip(kernel_diag, kernel_diag, 1);
        cv::filter2D(gradientImg, G_i[7], CV_32FC1, kernel_diag);	// -45
        cv::flip(kernel_diag, kernel_diag, 0);
        cv::filter2D(gradientImg, G_i[1], CV_32FC1, kernel_diag);	// 45
        cv::flip(kernel_diag, kernel_diag, 1);
        cv::filter2D(gradientImg, G_i[3], CV_32FC1, kernel_diag);	// 135

        std::vector<Mat>& C_i = getResponse(gradientImg, G_i);
        cv::filter2D(C_i[4], C_i[4], -1, kernel_hori);	// 180
        cv::flip(kernel_hori, kernel_hori, 1);
        cv::filter2D(C_i[0], C_i[0], -1, kernel_hori);	// 0
        cv::filter2D(C_i[6], C_i[6], -1, kernel_veri);	// -90
        cv::flip(kernel_veri, kernel_veri, 0);
        cv::filter2D(C_i[2], C_i[2], -1, kernel_veri);	// 90
        cv::filter2D(C_i[3], C_i[3], -1, kernel_diag);	// 135
        cv::flip(kernel_diag, kernel_diag, 0);
        cv::filter2D(C_i[5], C_i[5], -1, kernel_diag);	// -135
        cv::flip(kernel_diag, kernel_diag, 1);
        cv::filter2D(C_i[7], C_i[7], -1, kernel_diag);	// -45
        cv::flip(kernel_diag, kernel_diag, 0);
        cv::filter2D(C_i[1], C_i[1], -1, kernel_diag);	// 45

        Mat S_ = Mat::zeros(gradientImg.rows, gradientImg.cols, CV_32FC1);
        for (int i = 0; i < C_i.size(); ++i)
            S_ += C_i[i];

        double minVal, maxVal;
        cv::minMaxLoc(S_, &minVal, &maxVal);
        pencilStroke = (S_ - (float)minVal) / ((float)maxVal - (float)minVal);
        pencilStroke = 1 - pencilStroke;
    }

    void PencilDrawing::computeDistribution() {
        auto f1 = [this](const double& v)->double {
            //return 1.0 / delta_b * std::exp(-(255.0 - v) / delta_b);
            return 1.0 / delta_b * std::exp(-(1.0 - v / 255.0) / delta_b);
        };

        auto f2 = [this](const double& v)->double {
            if (v < u_a || v > u_b) return 0.0;
            else return 1.0 / (u_b - u_a);
        };

        auto f3 = [this](const double& v)->double {
            //return 1.0 / std::sqrt(2 * CV_PI * delta_d) * std::exp(-(v - u_d) * (v - u_d) / (2 * delta_d * delta_d));
            return 1.0 / std::sqrt(2 * CV_PI * delta_d) * std::exp(-(v / 255.0 - u_d) * (v - u_d) / (2 * delta_d * delta_d));
        };

        double total = 0;
        for (int i = 1; i < distribution.size(); ++i) {
            distribution[i] = weight_1 * f1(i) + weight_2 * f2(i) + weight_3 * f3(i);
            total += distribution[i];
        }

        std::for_each(distribution.begin(), distribution.end(), [total](double& ele) {ele /= total;});
    }

    // generateToneMap的辅助函数
    static uchar histEq(const std::vector<double>& hist, double value) {

        uchar minIdx = 0;
        for (int i = 0; i < hist.size(); ++i)
            if (std::abs(hist[i] - value) < std::abs(hist[minIdx] - value))
                minIdx = i;

        return minIdx;
    }

    /**
     * @param img 单通道图像 CV_8UC1
     * @param toneMap 生成的明暗图 CV_8UC1
     */
    void PencilDrawing::generateToneMap(const Mat& img, OUT_PARA Mat& toneMap) const {
        std::vector<double> histo(256, 0.0);
        const uchar* imgData = (uchar*)img.data;
        for (int r = 0; r < img.rows; ++r)
            for (int c = 0; c < img.cols; ++c)
                ++histo[imgData[r * img.cols + c]];

        std::for_each(histo.begin(), histo.end(), [&img](double& ele) {ele /= img.rows * img.cols;});

        toneMap = Mat::zeros(img.rows, img.cols, CV_8UC1);
        uchar* toneData = toneMap.data;
        for (int r = 0; r < img.rows; ++r)
            for (int c = 0; c < img.cols; ++c)
                toneData[r * img.cols + c] = histEq(distribution, histo[imgData[r * img.cols + c]]);
    }


    /**
     * @param tonemap_ CV_8UC1
     * @param pattern_ CV_8UC1
     * @param pencilTexture CV_32FC1
     */
    void PencilDrawing::generatePencilTexture(const Mat& toneMap_, const Mat& pattern_, OUT_PARA Mat& pencilTexture) const {
        const int size = toneMap_.cols * toneMap_.rows;
        Mat toneMap, pattern;
        toneMap_.convertTo(toneMap, CV_64FC1);
        pattern_.convertTo(pattern, CV_64FC1);
        cv::log(toneMap, toneMap);
        cv::log(pattern, pattern);
        int row = 0, col = 0;
        double* data = (double*)pattern.data;

        std::vector<Eigen::Triplet<double>> triplet;	// lnH(x)
        triplet.reserve(size);
        Eigen::SparseMatrix<double> lnH(size, size);
        for (int r = 0; r < pattern.rows; ++r)
            for (int c = 0; c < pattern.cols; ++c)
                triplet.push_back(Eigen::Triplet<double>(row++, col++, data[r * pattern.cols + c]));
        lnH.setFromTriplets(triplet.cbegin(), triplet.cend());

        triplet.clear();								// lnJ(x)
        row = 0, col = 0;
        data = (double*)toneMap.data;
        Eigen::SparseMatrix<double> lnJ(size, 1);
        for (int r = 0; r < toneMap.rows; ++r)
            for (int c = 0; c < toneMap.cols; ++c)
                triplet.push_back(Eigen::Triplet<double>(row++, col, data[r + toneMap.cols + c]));
        lnJ.setFromTriplets(triplet.cbegin(), triplet.cend());

        triplet.clear();
        Eigen::SparseMatrix<double> Dx(size, size);			// Dx
        row = 0, col = toneMap_.rows;
        for (int i = 0; i < size - toneMap_.rows; ++i) {
            triplet.emplace_back(i, row++, -1);
            triplet.emplace_back(i, col++, 1);
        }
        for (int i = size - toneMap_.rows; i < size; ++i)
            triplet.emplace_back(i, i, -1);
        Dx.setFromTriplets(triplet.cbegin(), triplet.cend());

        triplet.clear();
        Eigen::SparseMatrix<double> Dy(size, size);			// Dy
        row = 0, col = 1;
        for (int i = 0; i < size - 1; ++i) {
            triplet.emplace_back(i, row++, -1);
            triplet.emplace_back(i, col++, 1);
        }
        triplet.emplace_back(row, row, -1);
        Dy.setFromTriplets(triplet.cbegin(), triplet.cend());

        double lam = 0.2;
        Eigen::SparseMatrix<double> A = lam * (Dx * Dx.transpose() + Dy * Dy.transpose()) + lnH * lnH.transpose();
        Eigen::SparseMatrix<double> b_ = lnH.transpose() * lnJ;
        Eigen::VectorXd b = b_;
        Eigen::ConjugateGradient<Eigen::SparseMatrix<double>> cg;
        cg.setTolerance(1e-6);
        cg.setMaxIterations(100);
        cg.compute(A);
        Eigen::VectorXd beta = cg.solve(b);

        pencilTexture = Mat::zeros(pattern_.rows, pattern_.cols, CV_32FC1);
        float* textureData = (float*)pencilTexture.data;
        uchar* patternData = (uchar*)pattern_.data;
        for (int r = 0; r < pencilTexture.rows; ++r)
            for (int c = 0; c < pencilTexture.cols; ++c)
                textureData[r * pencilTexture.cols + c] = std::pow(patternData[r * pencilTexture.cols + c], beta(r * pencilTexture.cols + c));
    }
} // namespace picktime
