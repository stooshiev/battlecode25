from battlecode25.stubs import *

def turn():
    """
    MUST be defined for robot to run
    This function will be called at the beginning of every turn and should contain the bulk of your robot commands
    """
    loc = get_location()
    log(str(loc))
