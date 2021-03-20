# -*- coding: utf-8 -*-
# Author: Aditya Verma (adityaverma1001@gmail.com)
"""capstone70percent.ipynb

Automatically generated by Colaboratory.

Original file is located at
    https://colab.research.google.com/drive/1R9bO43z03esbtRyYI2hxMsmjB6ME70xF
"""

import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
import glob
import librosa
import numpy as np
import tensorflow as tf
from numpy import genfromtxt
import matplotlib.pyplot as plt
from tensorflow.keras import Sequential
from keras.utils.np_utils import to_categorical
from tensorflow.python.ops import rnn, rnn_cell
from tensorflow.keras.layers import Dense,Conv2D,MaxPooling2D,Flatten,Dropout


# %matplotlib inline
plt.style.use('ggplot')

tf.compat.v1.logging.set_verbosity(tf.compat.v1.logging.ERROR)
tf.get_logger().setLevel('ERROR')

def getFeaturesFromWav(wav_file):
    # feature set
    y, sr=librosa.load(wav_file)
    mfccs = np.mean(librosa.feature.mfcc(y, sr, n_mfcc=40).T,axis=0)
    melspectrogram = np.mean(librosa.feature.melspectrogram(y=y, sr=sr, n_mels=40,fmax=8000).T,axis=0)
    chroma_stft=np.mean(librosa.feature.chroma_stft(y=y, sr=sr,n_chroma=40).T,axis=0)
    chroma_cq = np.mean(librosa.feature.chroma_cqt(y=y, sr=sr,n_chroma=40).T,axis=0)
    chroma_cens = np.mean(librosa.feature.chroma_cens(y=y, sr=sr,n_chroma=40).T,axis=0)
    melspectrogram.shape,chroma_stft.shape,chroma_cq.shape,chroma_cens.shape,mfccs.shape
    features = np.reshape(np.vstack((mfccs,melspectrogram,chroma_stft,chroma_cq,chroma_cens)),(40,5))
    return features


def getModel():
    #forming model
    model=Sequential()
    #adding layers and forming the model
    model.add(Conv2D(64,kernel_size=5,strides=1,padding="Same",activation="relu",input_shape=(40,5,1)))
    model.add(MaxPooling2D(padding="same"))
    model.add(Conv2D(128,kernel_size=5,strides=1,padding="same",activation="relu"))
    model.add(MaxPooling2D(padding="same"))
    model.add(Dropout(0.3))
    model.add(Flatten())
    model.add(Dense(256,activation="relu"))
    model.add(Dropout(0.3))
    model.add(Dense(512,activation="relu"))
    model.add(Dropout(0.3))
    model.add(Dense(5,activation="softmax"))
    # model.add(Dense(10,activation="softmax"))
    # model.add(Dense(11,activation="softmax"))
    #compiling
    model.compile(optimizer="adam",loss="categorical_crossentropy",metrics=["accuracy"])
    #loading
    # model.load_weights('../Classifier/modelv1.hdf5')
    # model.load_weights('../Classifier/modelv0.hdf5')
    model.load_weights('../Classifier/modelv2.hdf5')
    return model


def getEnvClassification(wav_file):
    # TODO: get rid of children_playing,air_conditioner, engine_idling, street_music, drilling
    labels = ['air_conditioner','car_horn','children_playing','dog_bark','drilling','engine_idling','gun_shot','jackhammer','siren','street_music']
    new_labels = ['CAR HORN','DOG BARKING','GUN SHOT','CONSTRUCTION SOUNDS','SIREN / FIRE ALARM']
    wav_features = np.reshape(getFeaturesFromWav(wav_file), (1, 40, 5, 1))
    print("\n\n*******************************")
    model = getModel()
    out = model.predict(wav_features)
    label = labels[np.argmax(out)]
    new_label = new_labels[np.argmax(out)]
    print(new_label)
    print(out)
    if max(out[0]) < 0.82:
        label = "NONE"
        new_label = "NONE"
        print("NOT CONFIDENT ENOUGH")
        return ""
    print("*******************************\n\n")
    # if np.argmax(out) != 4:
    #     return ""
    return new_label
    # return label