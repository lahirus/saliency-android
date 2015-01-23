/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package saliency;


import static java.lang.Double.max;
import static java.lang.Math.abs;
import org.opencv.core.Core;
import static org.opencv.core.CvType.CV_16UC1;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_64F;
import static org.opencv.core.CvType.CV_64FC1;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import static org.opencv.highgui.Highgui.imread;
import static org.opencv.highgui.Highgui.imwrite;
import org.opencv.imgproc.Imgproc;

/**
 *
 * @author YuanXiquan
 */


public class saliency {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
        Mat input_img = imread("input_img/sea.jpg");  
        //fot temp test start
        Imgproc.resize(input_img, input_img, new Size(1980,1080), 0, 0, Imgproc.INTER_LINEAR);
        //fot temp test end
        if(input_img.cols() == 0){
            return;
        }

		//benchmark
        ///////////////////////step 1 : Extraction of Early Visual Deatures///////////////////////////////
        //intensity image: intensity_img
        Mat intensity_img = new Mat(input_img.rows(), input_img.cols(), CV_16UC1);
        //intensity = (R+G+B)/3
        int img_width = intensity_img.cols();
        int img_height = intensity_img.rows();
        int x, y;
        int i, c, s;
        int max_intensity = 0;
        for(x = 0; x < img_width; x++){
            for(y = 0; y < img_height; y++){
                int temp_intensity = ((int)input_img.get(y,x)[0] + (int)input_img.get(y,x)[1] + (int)input_img.get(y,x)[2])/3;
                intensity_img.put(y,x, temp_intensity);
                if(max_intensity < temp_intensity){
                    max_intensity = temp_intensity;
                }
            }
        }
        //create Guassian pyramid for intensity
        Mat[] i_gaussian_pyramid = new Mat[9];
        i_gaussian_pyramid[0] = intensity_img.clone();
        for(i = 0; i < 8; i++){
            i_gaussian_pyramid[i+1] = i_gaussian_pyramid[i].clone();
            Imgproc.pyrDown(i_gaussian_pyramid[i+1], i_gaussian_pyramid[i+1], new Size());
        }

        //create intensity feature map using center-surround differences
        Mat[][] intensity_feature_map = new Mat[3][2];
        for(c = 0; c < 3; c++){
            for(s = 0; s <2; s++){
                intensity_feature_map[c][s] = center_surround.main(i_gaussian_pyramid[c+2], i_gaussian_pyramid[s+c+5], 0);
            }
        }
		//benchmark
       //imwrite("intensity_feature_map_00.bmp", intensity_feature_map[0][0]);
        //get normalized color image by I.
        Mat norm_input_img = input_img.clone();
        norm_input_img.convertTo(norm_input_img, CV_64F);
        for(x = 0; x < img_width; x++){
            for(y = 0; y < img_height; y++){
                //normalization is only applied at the locations where I is larger than 1/10 of its maximum over entire image
                double[] temp = new double[3];
                if(intensity_img.get(y,x)[0]>(max_intensity/10)){
                    temp[0] = norm_input_img.get(y,x)[0]/intensity_img.get(y,x)[0];
                    temp[1] = norm_input_img.get(y,x)[1]/intensity_img.get(y,x)[0];
                    temp[2] = norm_input_img.get(y,x)[2]/intensity_img.get(y,x)[0];
                    norm_input_img.put(y,x,temp);
                }else{
                    temp[0] = 0;
                    temp[1] = 0;
                    temp[2] = 0;
                    norm_input_img.put(y,x,temp);
                }
            }    
        }
        //get R G B Y(Yellow) single color channel images
        Mat r_img = new Mat(input_img.rows(), input_img.cols(), CV_64FC1);
        Mat g_img = new Mat(input_img.rows(), input_img.cols(), CV_64FC1);
        Mat b_img = new Mat(input_img.rows(), input_img.cols(), CV_64FC1);
        Mat y_img = new Mat(input_img.rows(), input_img.cols(), CV_64FC1);
        //[0]: b [1]:g [2]:r
        for(x = 0; x < img_width; x++){
            for(y = 0; y < img_height; y++){
                //R = min(0,r-(g+b)/2)
                double temp_chroma = max(0, (norm_input_img.get(y,x)[2]-(norm_input_img.get(y,x)[1]+norm_input_img.get(y,x)[0])/2));
                r_img.put(y,x, temp_chroma);
                //G = max(0,g-(r+b)/2)
                temp_chroma = max(0, (norm_input_img.get(y,x)[1]-(norm_input_img.get(y,x)[2]+norm_input_img.get(y,x)[0])/2));
                g_img.put(y,x, temp_chroma);
                //B = max(0,b-(r+g)/2)
                temp_chroma = max(0, (norm_input_img.get(y,x)[0]-(norm_input_img.get(y,x)[2]+norm_input_img.get(y,x)[1])/2));
                b_img.put(y,x, temp_chroma);
                //Y = max(0,(r+g)/2-|r-g|/2-b)
                temp_chroma = max(0, ((norm_input_img.get(y,x)[2]+norm_input_img.get(y,x)[1])/2 -abs(norm_input_img.get(y,x)[2]+norm_input_img.get(y,x)[1])/2-norm_input_img.get(y,x)[0]));
                y_img.put(y,x, temp_chroma);
            }
        }        
        //create Gaussian pyramid for 4 color channels
        Mat[] b_gaussian_pyramid = new Mat[9];
        b_gaussian_pyramid[0] = b_img.clone();
        for(i = 0; i < 8; i++){
            b_gaussian_pyramid[i+1] = b_gaussian_pyramid[i].clone();
            Imgproc.pyrDown(b_gaussian_pyramid[i+1], b_gaussian_pyramid[i+1], new Size());
        }
        Mat[] g_gaussian_pyramid = new Mat[9];
        g_gaussian_pyramid[0] = g_img.clone();
        for(i = 0; i < 8; i++){
            g_gaussian_pyramid[i+1] = g_gaussian_pyramid[i].clone();
            Imgproc.pyrDown(g_gaussian_pyramid[i+1], g_gaussian_pyramid[i+1], new Size());
        }
        Mat[] r_gaussian_pyramid = new Mat[9];
        r_gaussian_pyramid[0] = r_img.clone();
        for(i = 0; i < 8; i++){
            r_gaussian_pyramid[i+1] = r_gaussian_pyramid[i].clone();
            Imgproc.pyrDown(r_gaussian_pyramid[i+1], r_gaussian_pyramid[i+1], new Size());
        }
        Mat[] y_gaussian_pyramid = new Mat[9];
        y_gaussian_pyramid[0] = y_img.clone();
        for(i = 0; i < 8; i++){
            y_gaussian_pyramid[i+1] = y_gaussian_pyramid[i].clone();
            Imgproc.pyrDown(y_gaussian_pyramid[i+1], y_gaussian_pyramid[i+1], new Size());
        }
        //create color feature map using center-surround differences
        //RG(c,s) = |(R(c)-G(c))(-)(G(c)-R(c))|
        Mat[][] rg_feature_map = new Mat[3][2];
        for(c = 0; c < 3; c++){
            for(s = 0; s <2; s++){
                Mat r_minus_g = r_gaussian_pyramid[c+2].clone();
                Core.subtract(r_gaussian_pyramid[c+2], g_gaussian_pyramid[c+2], r_minus_g);
                Mat g_minus_r = g_gaussian_pyramid[s+c+5].clone();
                Core.subtract(g_gaussian_pyramid[s+c+5], r_gaussian_pyramid[s+c+5], g_minus_r);
                rg_feature_map[c][s] = center_surround.main(r_minus_g, g_minus_r, 1);
            }
        }
        //BY(c,s) = |(B(c)-Y(c))(-)(Y(c)-B(c))|
        Mat[][] by_feature_map = new Mat[3][2];
        for(c = 0; c < 3; c++){
            for(s = 0; s <2; s++){
                Mat b_minus_g = b_gaussian_pyramid[c+2].clone();
                Core.subtract(b_gaussian_pyramid[c+2], y_gaussian_pyramid[c+2], b_minus_g);
                Mat y_minus_b = y_gaussian_pyramid[s+c+5].clone();
                Core.subtract(y_gaussian_pyramid[s+c+5], b_gaussian_pyramid[s+c+5], y_minus_b);
                by_feature_map[c][s] = center_surround.main(b_minus_g, y_minus_b, 1);
            }
        }
		//benchmark
        //create oriented Gabor pyramid from intensity image
        int kernel_size = 10;//31;//adjust value according to reference
        double sigma = 3;//default: σ = 0.56 λ.  the larger σ, the support of the Gabor function  and the number of visible parallel excitatory and inhibitory stripe zones increases.
        double[] theta = new double[4];
        theta[0] = 0;
        theta[1] = Math.PI/4;
        theta[2] = Math.PI/2;
        theta[3] = Math.PI*3/4;
        double lambda = 5;//36; minimum 3
        double gamma = 0.5;//0.02;
       // double psi = 0;
        Mat[][] gabor_pyramid = new Mat[4][9];
        int theta_index;
        for(theta_index = 0; theta_index < 4; theta_index++){
            Mat gabor_kernel = Imgproc.getGaborKernel(new Size(kernel_size,kernel_size), sigma, theta[theta_index], lambda, gamma);
            //gabor_pyramid[theta_index][0] = intensity_img.clone();
            for(i = 0; i < 9; i++){
                //gabor_pyramid[theta_index][i] = gabor_pyramid[theta_index][i].clone();
                gabor_pyramid[theta_index][i] = i_gaussian_pyramid[i].clone();
                Imgproc.filter2D(i_gaussian_pyramid[i], gabor_pyramid[theta_index][i], -1, gabor_kernel);
                //Imgproc.resize(gabor_pyramid[theta_index][i], gabor_pyramid[theta_index][i], new Size(), 0.5, 0.5, Imgproc.INTER_AREA);
            }
        }
        //imwrite("gabor_pyramid_01.bmp", gabor_pyramid[0][1]);
        //imwrite("gabor_pyramid_11.bmp", gabor_pyramid[1][1]);
        //imwrite("gabor_pyramid_21.bmp", gabor_pyramid[2][1]);
        //imwrite("gabor_pyramid_31.bmp", gabor_pyramid[3][1]);
        //imwrite("gabor_pyramid_03.bmp", gabor_pyramid[0][3]);
        //get orientation feature map using center-surround differences
        Mat[][][] orientation_feature_map = new Mat[4][3][2];
        for(theta_index = 0; theta_index < 4; theta_index++){
            for(c = 0; c < 3; c++){
                for(s = 0; s <2; s++){
                    orientation_feature_map[theta_index][c][s] = center_surround.main(gabor_pyramid[theta_index][c+2], gabor_pyramid[theta_index][s+c+5], 0);
                }
            }
        }
		//benchmark
        //imwrite("orientation_test_00.bmp", orientation_feature_map[0][0][0]);
        ///////////////////////step 2 : the saliency map///////////////////////////////
        //get intensity conspicuity map
        Mat intensity_conspicuity_map = Mat.zeros(intensity_feature_map[2][0].size(), CV_16UC1);  
        for(c = 0; c < 3; c++){
            for(s = 0; s <2; s++){
                Mat norm_out = map_norm.main(intensity_feature_map[c][s]);
                Mat resized_feature_map = Mat.zeros(intensity_feature_map[2][0].size(), CV_16UC1);              
                Imgproc.resize(norm_out, resized_feature_map, intensity_feature_map[2][0].size(), 0, 0, Imgproc.INTER_LINEAR);
                Core.addWeighted(intensity_conspicuity_map, 1, resized_feature_map, 1.0/6, 0, intensity_conspicuity_map);
                /*if(c == 0 && s == 0){
                    imwrite("in.bmp", intensity_feature_map[c][s]);
                    imwrite("map_norm.bmp",norm_out);
                    imwrite("resized_feature_map.bmp", resized_feature_map);
                }*/
            }
        }
		//benchmark
        //Core.normalize(intensity_conspicuity_map, intensity_conspicuity_map, 0, 255, Core.NORM_MINMAX);
        //imwrite("intensity_conspicuity_map.bmp", intensity_conspicuity_map);
        //get color conspicuity map
        for(c = 0; c < 3; c++){
            for(s = 0; s <2; s++){
                Core.normalize(rg_feature_map[c][s], rg_feature_map[c][s], 0, 255, Core.NORM_MINMAX);
                rg_feature_map[c][s].convertTo(rg_feature_map[c][s], CV_16UC1);
                Core.normalize(by_feature_map[c][s], by_feature_map[c][s], 0, 255, Core.NORM_MINMAX);
                by_feature_map[c][s].convertTo(by_feature_map[c][s], CV_16UC1);
            }
        }
        //imwrite("test_rg.bmp",rg_feature_map[0][0]);      
        Mat color_conspicuity_map = Mat.zeros(rg_feature_map[2][0].size(), CV_16UC1);  
        for(c = 0; c < 3; c++){
            for(s = 0; s <2; s++){
                Mat norm_out = map_norm.main(rg_feature_map[c][s]);
                Mat resized_feature_map = Mat.zeros(rg_feature_map[2][0].size(), CV_16UC1);              
                Imgproc.resize(norm_out, resized_feature_map, rg_feature_map[2][0].size(), 0, 0, Imgproc.INTER_LINEAR);
                Core.addWeighted(color_conspicuity_map, 1, resized_feature_map, 1.0/12, 0, color_conspicuity_map);
                norm_out = map_norm.main(by_feature_map[c][s]);
                resized_feature_map = Mat.zeros(by_feature_map[2][0].size(), CV_16UC1);              
                Imgproc.resize(norm_out, resized_feature_map, by_feature_map[2][0].size(), 0, 0, Imgproc.INTER_LINEAR);
                Core.addWeighted(color_conspicuity_map, 1, resized_feature_map, 1.0/12, 0, color_conspicuity_map);
            }
        }        
		//benchmark
        //get orientation conspicuity map
        Mat orientation_conspicuity_map_0 = Mat.zeros(orientation_feature_map[0][2][0].size(), CV_16UC1);  
        for(c = 0; c < 3; c++){
            for(s = 0; s <2; s++){
                Mat norm_out = map_norm.main(orientation_feature_map[0][c][s]);
                Mat resized_feature_map = Mat.zeros(orientation_feature_map[0][2][0].size(), CV_16UC1);              
                Imgproc.resize(norm_out, resized_feature_map, orientation_feature_map[0][2][0].size(), 0, 0, Imgproc.INTER_LINEAR);
                Core.addWeighted(orientation_conspicuity_map_0, 1, resized_feature_map, 1.0/6, 0, orientation_conspicuity_map_0);
            }
        }
       
        Mat orientation_conspicuity_map_1 = Mat.zeros(orientation_feature_map[1][2][0].size(), CV_16UC1);  
        for(c = 0; c < 3; c++){
            for(s = 0; s <2; s++){
                Mat norm_out = map_norm.main(orientation_feature_map[1][c][s]);
                Mat resized_feature_map = Mat.zeros(orientation_feature_map[1][2][0].size(), CV_16UC1);              
                Imgproc.resize(norm_out, resized_feature_map, orientation_feature_map[1][2][0].size(), 0, 0, Imgproc.INTER_LINEAR);
                Core.addWeighted(orientation_conspicuity_map_1, 1, resized_feature_map, 1.0/6, 0, orientation_conspicuity_map_1);
            }
        }
        Mat orientation_conspicuity_map_2 = Mat.zeros(orientation_feature_map[2][2][0].size(), CV_16UC1);  
        for(c = 0; c < 3; c++){
            for(s = 0; s <2; s++){
                Mat norm_out = map_norm.main(orientation_feature_map[2][c][s]);
                Mat resized_feature_map = Mat.zeros(orientation_feature_map[2][2][0].size(), CV_16UC1);              
                Imgproc.resize(norm_out, resized_feature_map, orientation_feature_map[2][2][0].size(), 0, 0, Imgproc.INTER_LINEAR);
                Core.addWeighted(orientation_conspicuity_map_2, 1, resized_feature_map, 1.0/6, 0, orientation_conspicuity_map_2);
            }
        }
        Mat orientation_conspicuity_map_3 = Mat.zeros(orientation_feature_map[3][2][0].size(), CV_16UC1);  
        for(c = 0; c < 3; c++){
            for(s = 0; s <2; s++){
                Mat norm_out = map_norm.main(orientation_feature_map[3][c][s]);
                Mat resized_feature_map = Mat.zeros(orientation_feature_map[3][2][0].size(), CV_16UC1);              
                Imgproc.resize(norm_out, resized_feature_map, orientation_feature_map[3][2][0].size(), 0, 0, Imgproc.INTER_LINEAR);
                Core.addWeighted(orientation_conspicuity_map_3, 1, resized_feature_map, 1.0/6, 0, orientation_conspicuity_map_3);
            }
        } 
        Mat orientation_conspicuity_map = Mat.zeros(orientation_feature_map[0][2][0].size(), CV_16UC1);  
        Core.addWeighted(orientation_conspicuity_map, 1, map_norm.main(orientation_conspicuity_map_0), 1.0/4, 0, orientation_conspicuity_map);
        Core.addWeighted(orientation_conspicuity_map, 1, map_norm.main(orientation_conspicuity_map_1), 1.0/4, 0, orientation_conspicuity_map);
        Core.addWeighted(orientation_conspicuity_map, 1, map_norm.main(orientation_conspicuity_map_2), 1.0/4, 0, orientation_conspicuity_map);
        Core.addWeighted(orientation_conspicuity_map, 1, map_norm.main(orientation_conspicuity_map_3), 1.0/4, 0, orientation_conspicuity_map);
       //benchmark
        Mat saliency = Mat.zeros(intensity_conspicuity_map.size(), CV_16UC1);  
        Core.addWeighted(saliency, 1, map_norm.main(intensity_conspicuity_map), 1.0/3, 0, saliency);
        Core.addWeighted(saliency, 1, map_norm.main(color_conspicuity_map), 1.0/3, 0, saliency);
        Core.addWeighted(saliency, 1, map_norm.main(orientation_conspicuity_map), 1.0/3, 0, saliency);
        //benchmark
        Core.normalize(saliency, saliency, 0, 255, Core.NORM_MINMAX);
        //fot temp test
        Imgproc.resize(saliency, saliency, new Size(720,480), 0, 0, Imgproc.INTER_LINEAR);
        imwrite("saliency.bmp", saliency);
        
        Core.normalize(intensity_conspicuity_map, intensity_conspicuity_map, 0, 255, Core.NORM_MINMAX);
        Imgproc.resize(intensity_conspicuity_map, intensity_conspicuity_map, new Size(720,480), 0, 0, Imgproc.INTER_LINEAR);
        imwrite("intensity_conspicuity_map.bmp", intensity_conspicuity_map);
        Core.normalize(color_conspicuity_map, color_conspicuity_map, 0, 255, Core.NORM_MINMAX);
        Imgproc.resize(color_conspicuity_map, color_conspicuity_map, new Size(720,480), 0, 0, Imgproc.INTER_LINEAR);
        imwrite("color_conspicuity_map.bmp", color_conspicuity_map);
        Core.normalize(orientation_conspicuity_map, orientation_conspicuity_map, 0, 255, Core.NORM_MINMAX);
        Imgproc.resize(orientation_conspicuity_map, orientation_conspicuity_map, new Size(720,480), 0, 0, Imgproc.INTER_LINEAR);
        imwrite("orientation_conspicuity_map.bmp", orientation_conspicuity_map);
        Imgproc.resize(input_img, input_img, new Size(720,480), 0, 0, Imgproc.INTER_LINEAR);
        imwrite("input_img.bmp", input_img);
        //for testing algorithm
        /*
        Mat temp1 = Mat.zeros(intensity_conspicuity_map.size(), CV_16UC1);
        temp1 = map_norm.main(intensity_conspicuity_map);
        Core.normalize(temp1, temp1, 0, 255, Core.NORM_MINMAX);
        Imgproc.resize(temp1, temp1, new Size(720,480), 0, 0, Imgproc.INTER_LINEAR);
        imwrite("intensity.bmp", temp1);
        temp1 = map_norm.main(color_conspicuity_map);
        Core.normalize(temp1, temp1, 0, 255, Core.NORM_MINMAX);
        Imgproc.resize(temp1, temp1, new Size(720,480), 0, 0, Imgproc.INTER_LINEAR);
        imwrite("color.bmp", temp1);
        temp1 = map_norm.main(orientation_conspicuity_map);
        Core.normalize(temp1, temp1, 0, 255, Core.NORM_MINMAX);
        Imgproc.resize(temp1, temp1, new Size(720,480), 0, 0, Imgproc.INTER_LINEAR);
        imwrite("orientation.bmp", temp1);
        
        Mat temp2 = Mat.zeros(orientation_conspicuity_map_0.size(), CV_16UC1);
        temp2 = map_norm.main(orientation_conspicuity_map_0);
        Core.normalize(temp2, temp2, 0, 255, Core.NORM_MINMAX);
        Imgproc.resize(temp2, temp2, new Size(720,480), 0, 0, Imgproc.INTER_LINEAR);
        imwrite("orientation_conspicuity_map_0.bmp", temp2);
        temp2 = map_norm.main(orientation_conspicuity_map_1);
        Core.normalize(temp2, temp2, 0, 255, Core.NORM_MINMAX);
        Imgproc.resize(temp2, temp2, new Size(720,480), 0, 0, Imgproc.INTER_LINEAR);
        imwrite("orientation_conspicuity_map_1.bmp", temp2);
        temp2 = map_norm.main(orientation_conspicuity_map_2);
        Core.normalize(temp2, temp2, 0, 255, Core.NORM_MINMAX);
        Imgproc.resize(temp2, temp2, new Size(720,480), 0, 0, Imgproc.INTER_LINEAR);
        imwrite("orientation_conspicuity_map_2.bmp", temp2);
        temp2 = map_norm.main(orientation_conspicuity_map_3);
        Core.normalize(temp2, temp2, 0, 255, Core.NORM_MINMAX);
        Imgproc.resize(temp2, temp2, new Size(720,480), 0, 0, Imgproc.INTER_LINEAR);
        imwrite("orientation_conspicuity_map_3.bmp", temp2);
        */
    }
}

