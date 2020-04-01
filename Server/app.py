import io
import sys
import dash
import base64
import librosa
import requests
import subprocess
import numpy as np
import dash_daq as daq
sys.path.append('../Classifier')
import env_classifier as ec
import dash_html_components as html
from scipy.io.wavfile import read, write
from flask import Flask, request, redirect


recentAngle = 90
server = Flask("Server")
filenameMP4 = "newwav.mp4"
filenameWAV = "newwav.wav"
external_stylesheets = ['https://codepen.io/chriddyp/pen/bWLwgP.css']

@server.route('/')
def hello_world():
    # TODO: simply reroute this to /visualizer
    return redirect('visualizer')

visualizer = dash.Dash(name="visualizer", server=server,
                        url_base_pathname='/')
visualizer.layout = html.Div([
    daq.Joystick(
        id='my-joystick',
        label="Default",
        angle=90
    ),
    html.Div(id='joystick-output')
])

@visualizer.callback(
    dash.dependencies.Output('joystick-output', 'children'),
    [dash.dependencies.Input('my-joystick', 'angle'),
     dash.dependencies.Input('my-joystick', 'force')])
def update_output(angle, force):
    global recentAngle
    if(angle and force):
        recentAngle = angle
    return ['Angle is {}'.format(angle),
            html.Br(),
            'Force is {}'.format(force)]

@server.route('/getangle', methods=['GET'])
def get_data():
    global recentAngle
    return(str(recentAngle))

@server.route('/env_classifier', methods=['POST'])
def classify():
    req_data = request.get_json()
    print("\n\n***************************RECEIVED REQUEST***************************************")
    print(recentAngle)
    if req_data:
        recording = req_data['recording']
        my_str_as_bytes = base64.b64decode(recording)
        newf = open(filenameMP4, "wb")
        newf.write(my_str_as_bytes)
        newf.close()
        command = "ffmpeg -y -hide_banner -loglevel panic -i {} -vn -acodec pcm_s16le -ar 44100 -ac 2 {}".format(filenameMP4, filenameWAV)
        subprocess.call(command, shell=True)
    else:
        print("EMPTY REQUEST")
        return ""

    wav_file = filenameWAV
    label = ec.getEnvClassification(wav_file)
    return(str(label))

if __name__ == '__main__':
    visualizer.run_server(debug=True)
