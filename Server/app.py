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
classif = ''
sendAngle=''

server = Flask("Server")
filenameMP4 = "newwav.mp4"
filenameWAV = "newwav.wav"
external_stylesheets = ['https://codepen.io/chriddyp/pen/bWLwgP.css',
{
        'href': 'https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/css/bootstrap.min.css',
        'rel': 'stylesheet',
        'integrity': 'sha384-MCw98/SFnGE8fJT3GXwEOngsV7Zt27NXFoaoApmYm81iuXoPkFOJwJ8ERdknLPMO',
        'crossorigin': 'anonymous'
    }
]

@server.route('/')
def hello_world():
    # TODO: simply reroute this to /visualizer
    return redirect('visualizer')

visualizer = dash.Dash(name="visualizer", server=server,
                        url_base_pathname='/',
                external_stylesheets=external_stylesheets)

visualizer.layout = html.Div(className='main', children=[
    html.H1(
        children='hARk',
        className=['text-center title'],
        style={"fontSize": "18em"}
    ),

    html.H2(
        children='Drag the joystick to select the direction of incoming noise',
        className='instr text-center',
        style={"fontSize": "5em"}
    ),
    html.Div([
    daq.Joystick(
        id='my-joystick',
        angle=90,
        size=300,
        className='stick col-md-6 justify-content-center align-items-center'
    ),
    html.Div(id='joystick-output', className='angle col-md-6', style={"fontSize": "8em"}),
],className='joystick row')

    #html.Div([],className="")
])

@visualizer.callback(
    dash.dependencies.Output('joystick-output', 'children'),
    [dash.dependencies.Input('my-joystick', 'angle')])
def update_output(angle):
    global recentAngle
    global sendAngle
    if(angle):
        recentAngle = angle
    return ['The Angle is set to: {:.0f}'.format(angle),
            html.Br()]

def send_angle():
    global sendAngle
    return ['{}'.format(sendangle),
            html.Br()]

def update_output():
    global classif
    return ['{}'.format(classif),
            html.Br()]


@server.route('/getangle', methods=['GET'])
def get_data():
    global recentAngle
    global sendAngle
    sendAngle = 'The angle: '+str(recentAngle)+' has been sent to the device.'
    return(str(recentAngle))

@server.route('/env_classifier', methods=['POST'])
def classify():
    global classif
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
    classif = 'SUDDEN NOISE DETECTED! Please wait while it is classified...'
    label = ec.getEnvClassification(wav_file)
    classif = 'The noise just turned out to be: '+str(label)
    return(str(label))

if __name__ == '__main__':
    visualizer.run_server(debug=True)
