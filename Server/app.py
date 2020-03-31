from flask import Flask, request, redirect
import requests
import dash
import dash_daq as daq
import dash_html_components as html

server = Flask("Server")
recentAngle = 0
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
        angle=0
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


if __name__ == '__main__':
    visualizer.run_server(debug=True)