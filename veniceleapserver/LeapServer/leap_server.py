"""
Get data from Leap sensor and send data to veniceipc

Usage: python leap.py localhost 54444
"""
import socket
import zlib
import sys
import argparse
import time
from collections import defaultdict
import xml.etree.ElementTree as ET
import Leap


class VeniceIPCSocket(object):
    "Create socket and send events to veniceIPC"

    def __init__(self, host, port):
        send = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        send.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        send.connect((host, port))
        self.venicesocket = send
        self.fields = self.load_field_names('leap.xml')
        print"connected to veniceIPC"

    def load_field_names(self, xml_file):
        # parse xml file to get field names
        tree = ET.parse(xml_file)
        root = tree.getroot()
        slots = [item.attrib['name'] for item in root[0][0]]

        return slots

    def send_message(self, mydict):
        """
        prepare message for completing
        """
        mystring = []
        for key in self.fields:
            if key in mydict.keys():
                mystring.append(', '.join(mydict[key]))
            else:
                #if a MFVec3f field is missing, set field length to 0
                mystring.append('0')

        msg = ', '.join(mystring)

        # add checksum, timestamp and termination char
        check_sum = zlib.adler32(msg) & 0xffffffff
        completed_message = (str(check_sum) + ', ' + msg + '\n')
        self.venicesocket.sendall(completed_message)


class Listener(Leap.Listener):
    """ get frame data from leap sensor """

    finger_names = ['Thumb', 'Index', 'Middle', 'Ring', 'Pinky']
    bone_names = ['Metacarpal', 'Proximal', 'Intermediate', 'Distal']
    state_names = ['STATE_INVALID', 'STATE_START', 'STATE_UPDATE', 'STATE_END']

    def __init__(self, vsocket):
        super(Listener, self).__init__()
        self.vsocket = vsocket
        self.mydict = dict()

    def on_init(self, controller):
        """initialize"""
        print 'Initialized'

    def on_connect(self, controller):
        """ connected """
        print 'Connected'

        # Enable gestures
        controller.enable_gesture(Leap.Gesture.TYPE_CIRCLE)
        controller.enable_gesture(Leap.Gesture.TYPE_KEY_TAP)
        controller.enable_gesture(Leap.Gesture.TYPE_SCREEN_TAP)
        controller.enable_gesture(Leap.Gesture.TYPE_SWIPE)
        controller.config.set('Gesture.ScreenTap.MinForwardVelocity', 30.0)
        controller.config.set('Gesture.ScreenTap.HistorySeconds', .5)
        controller.config.set('Gesture.ScreenTap.MinDistance', 1.0)
        controller.config.save()

    def on_disconnect(self, controller):
        """ Note: not dispatched when running in a debugger."""
        print 'Disconnected'

    def on_exit(self, controller):
        """ controller exit """
        print 'Exited'

    def on_frame(self, controller):
        """ Get the most recent frame and report some basic information """
        finger_angles = ['ThumbIndexAngle', 'IndexMidAngle', 'MidRingAngle',
                         'RingPinkyAngle']
        bone_angles = ['MetaProxAngle', 'ProxInterAngle', 'InterDistAngle']
        bone_direc = ['MetaDirection', 'ProxDirection', 'InterDirection',
                      'DistDirection']
        bone_len = ['MetaLength', 'ProxLength', 'InterLength', 'DistLength']
        bone_prevJoint = ['MetaPrevJoint', 'ProxPrevJoint', 'InterPrevJoint',
                          'DistPrevJoint']
        bone_nextJoint = ['MetaNextJoint', 'ProxNextJoint', 'InterNextJoint',
                          'DistNextJoint']

        frame = controller.frame()
        mydict = defaultdict(list)
        hand_num = len(frame.hands)

        for hand in frame.hands:
            arm = hand.arm
            mydict['ArmDirection'].append(str(arm.direction)[1:-1])
            mydict['ElbowPosition'].append(str(arm.elbow_position)[1:-1])
            mydict['ArmWidth'].append(str(arm.width))
            mydict['WristPosition'].append(str(arm.wrist_position)[1:-1])
            displacement = arm.wrist_position - arm.elbow_position
            mydict['ArmLength'].append(str(displacement.magnitude))

            mydict['HandID'].append(str(hand.id))
            mydict['HandConfid'].append(str(hand.confidence))
            mydict['GrabStrength'].append(str(hand.grab_strength))
            mydict['PalmNormal'].append(str(hand.palm_normal)[1:-1])
            mydict['PalmYaw'].append(str(hand.direction.yaw))
            mydict['PalmRoll'].append(str(hand.direction.roll))
            mydict['PalmPitch'].append(str(hand.direction.pitch))
            mydict['PalmPosition'].append(str(hand.palm_position)[1:-1])
            mydict['HandDirection'].append(str(hand.direction)[1:-1])
            mydict['PalmVelocity'].append(str(hand.palm_velocity)[1:-1])
            mydict['PinchStrength'].append(str(hand.pinch_strength))
            mydict['SphereCenter'].append(str(hand.sphere_center)[1:-1])
            mydict['SphereRadius'].append(str(hand.sphere_radius))
            mydict['PalmWidth'].append(str(hand.palm_width))

            if hand.is_left:
                mydict['HandType'].append('Left hand')
            else:
                mydict['HandType'].append('Right hand')

            # Get fingers
            # finger_num = 5 * hand_num
            prox_direc = []
            for finger in hand.fingers:
                prox_direc.append(finger.bone(1).direction)
                mydict['FingerType'].append(self.finger_names[finger.type])
                mydict['FingerLength'].append(str(finger.length))
                mydict['FingerWidth'].append(str(finger.width))
                mydict['TipDirection'].append(str(finger.direction)[1:-1])
                mydict['TipPosition'].append(str(finger.tip_position)[1:-1])

                for ix, (d, l, p, n) in enumerate(zip(
                        bone_direc, bone_len, bone_prevJoint, bone_nextJoint)):

                    mydict[d].append(str(finger.bone(ix).direction)[1:-1])
                    mydict[l].append(str(finger.bone(ix).length))
                    mydict[p].append(str(finger.bone(ix).prev_joint)[1:-1])
                    mydict[n].append(str(finger.bone(ix).next_joint)[1:-1])

                for ix, name in enumerate(bone_angles):
                    angle = finger.bone(ix).direction.angle_to(
                        finger.bone(ix+1).direction)
                    mydict[name].append(str(angle*180/Leap.PI))

            for index, angle in enumerate(finger_angles):
                mydict[angle].append(str(prox_direc[index].angle_to(
                    prox_direc[index+1])*180/Leap.PI))

        # Get tools
        tool_num = len(frame.tools)
        for tool in frame.tools:
            mydict['ToolPosition'].append(str(tool.tip_position)[1:-1])
            mydict['ToolDirection'].append(str(tool.direction)[1:-1])

        # Get gestures
        gesture_num = len(frame.gestures())
        for gesture in frame.gestures():
            mydict['GestureState'].append(str(self.state_names[gesture.state]))
            mydict['GestureDuration'].append(str(gesture.duration))

            if gesture.type == Leap.Gesture.TYPE_CIRCLE:
                #circle = Leap.CircleGesture(gesture)
                mydict['GestureType'].append('circle')
                #circle has no position attribute
                mydict['GesturePosition'].append(str([-1.0, -1.0, -1.0])[1:-1])

            if gesture.type == Leap.Gesture.TYPE_SWIPE:
                mydict['GestureType'].append('swip')
                swipe = Leap.SwipeGesture(gesture)
                mydict['GesturePosition'].append(str(swipe.position)[1:-1])

            if gesture.type == Leap.Gesture.TYPE_KEY_TAP:
                mydict['GestureType'].append('key tap')
                keytap = Leap.KeyTapGesture(gesture)
                mydict['GesturePosition'].append(str(keytap.position)[1:-1])

            if gesture.type == Leap.Gesture.TYPE_SCREEN_TAP:
                mydict['GestureType'].append('screen tap')
                screentap = Leap.ScreenTapGesture(gesture)
                mydict['GesturePosition'].append(str(screentap.position)[1:-1])

        # insert field length for  multi-field values
        for _, value in mydict.iteritems():
            value.insert(0, str(len(value)))

        # add sing field values
        mydict['GestureNum'].append(str(gesture_num))
        mydict["LeapTime"].append(str(int(time.time()*1000)))
        mydict['FrameID'].append(str(frame.id))
        mydict['HandNum'].append(str(hand_num))
        mydict['ToolNum'].append(str(tool_num))

        self.vsocket.send_message(mydict)
        print 'frame rate: ', frame.current_frames_per_second
        mydict.clear()


def main():
    """ Get data from Leap sensor and send it to VeniceIPC """

    parser = argparse.ArgumentParser()
    parser.add_argument('-host', dest='host', default='localhost',
                        help='localhost')
    parser.add_argument('-port', dest='port', default=5444, type=int,
                        help="port number")
    args = parser.parse_args()

    host = args.host
    port = args.port

    mysocket = VeniceIPCSocket(host, port)

    # Create a listener and controller
    listener = Listener(mysocket)
    controller = Leap.Controller()
    controller.set_policy_flags(Leap.Controller.POLICY_BACKGROUND_FRAMES)

    # Have the listener receive events from the controller
    controller.add_listener(listener)

    # Keep this process running until Enter is pressed
    print 'Press Enter to quit...'
    sys.stdin.readline()

    # Remove the listener when done
    controller.remove_listener(listener)


if __name__ == '__main__':
    main()
