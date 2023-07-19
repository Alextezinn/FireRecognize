package com.samsung.itschool.firereconize;

import static org.junit.Assert.*;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.pytorch.Tensor;

public class ClassifierTest {
    @Mock
    private Bitmap bitmap;

    @Before
    public void setUp() {
        bitmap = Mockito.mock(Bitmap.class);
    }


    @Test
    public void preprocessSuccess() {
        Bitmap mockBitmap = Mockito.mock(Bitmap.class);
        Mockito.when(bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)).thenReturn(mockBitmap);
//        bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inMutable = true;
//        Bitmap mockBitmap = Mockito.mock(Bitmap.class);
//
//        BitmapFactory.decodeFile("res/drawable/test.jpg", options);
//        Bitmap mockBitmap = Mockito.mock(Bitmap.class);
       // Mockito.when(BitmapFactory.decodeFile(Mockito.anyString(), ArgumentMatchers.eq(options))).thenReturn(mockBitmap);
//        BitmapFactory op = new BitmapFactory();
//        Bitmap imageBitmap = op.decodeFile("res/drawable/test.jpg", options);
//
//        Classifier classifier = new Classifier("assets/model.pth"); // initialize the classifier
//        Tensor preproccess = classifier.preprocess(mgBmp);
//        System.out.println(preproccess); //print the preprocessed tensor
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inMutable = true;
//        Bitmap mockBitmap = Mockito.mock(Bitmap.class);
//        Mockito.when(BitmapFactory.decodeFile(Mockito.anyString(), Mockito.any())).thenReturn(mockBitmap);
//        BitmapFactory op = new BitmapFactory();
//        Bitmap imageBitmap = op.decodeFile("res/drawable/test.jpg");
//
//        Classifier classifier = null;
//        Tensor preproccess = classifier.preprocess(imageBitmap);
//        System.out.println(preproccess);
    }
}