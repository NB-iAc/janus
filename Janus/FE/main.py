from flask import Flask, render_template, request, redirect, url_for, session, jsonify, flash
import uuid
import json
import os
import hashlib
import secrets
import requests
import logging
from logging.handlers import RotatingFileHandler
from dotenv import load_dotenv
from datetime import datetime, timedelta
from flask_mail import Mail, Message
from authlib.integrations.flask_client import OAuth
import logging
import json
from logging.handlers import RotatingFileHandler
logger = logging.getLogger('janus')
console_handler = logging.StreamHandler()
console_handler.setLevel(logging.INFO)
console_formatter = logging.Formatter('%(levelname)s - %(message)s')
console_handler.setFormatter(console_formatter)
logger.addHandler(console_handler)
API_BASE_URL = os.getenv('API_BASE_URL',)
logger.info("JANUS application starting up")
logger.info(f"API Base URL: {API_BASE_URL}")
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger('janus')
logger.setLevel(logging.DEBUG)
load_dotenv()
app = Flask(__name__, template_folder='../templates', static_folder='../static')
app.secret_key = "REDACTEDKEY"
app.config['MAIL_SERVER'] = 'smtp.gmail.com'  
app.config['MAIL_PORT'] = 587
app.config['MAIL_USE_TLS'] = True
app.config['MAIL_USERNAME'] = os.getenv('MAIL_USERNAME')
app.config['MAIL_PASSWORD'] = os.getenv('MAIL_PASSWORD')
app.config['MAIL_DEFAULT_SENDER'] = os.getenv('MAIL_USERNAME')
mail = Mail(app)
oauth = OAuth(app)
google = oauth.register(
    name='google',
    client_id=os.getenv('GOOGLE_CLIENT_ID'),
    client_secret=os.getenv('GOOGLE_CLIENT_SECRET'),
    server_metadata_url="https://accounts.google.com/.well-known/openid-configuration",
    jwks_uri="https://www.googleapis.com/oauth2/v3/certs",
    access_token_url='https://accounts.google.com/o/oauth2/token',
    access_token_params=None,
    authorize_url='https://accounts.google.com/o/oauth2/auth',
    authorize_params=None,
    api_base_url='https://www.googleapis.com/oauth2/v1/',
    client_kwargs={'scope': 'openid email profile'},
    userinfo_endpoint="https://openidconnect.googleapis.com/v1/userinfo",
    issuer="https://accounts.google.com"
)
@app.route('/api/proxy/building/<building_id>', methods=['GET'])
def proxy_get_building_data(building_id):
    try:
        response = api_get(f"/api/building-data/{building_id}")
        if not response or response.status_code != 200:
            return jsonify({
                "success": False,
                "error": f"Failed to fetch building data: {response.text if response else 'No response'}"
            }), 400
        return jsonify(response.json())
    except Exception as e:
        logger.error(f"Error fetching building data: {str(e)}", exc_info=True)
        return jsonify({
            "success": False,
            "error": f"Server error: {str(e)}"
        }), 500
@app.route("/api/protected/buildings/inaccessible", methods=["GET"])
def get_all_access_buildings():
    logger.info("Get all buildings request for access request modal")
    try:
        response = api_get("/api/protected/buildings/inaccessible")
        if response and response.status_code == 200:
            buildings = response.json()
            logger.info(f"Retrieved {len(buildings)} buildings for access request modal")
            return jsonify(buildings), 200
        else:
            logger.error(f"Failed to retrieve buildings: {response.status_code if response else 'No response'}")
            return jsonify({"error": "Failed to retrieve buildings"}), response.status_code if response else 500
    except Exception as e:
        logger.error(f"Error getting all buildings: {str(e)}", exc_info=True)
        return jsonify({"error": f"Failed to get buildings: {str(e)}"}), 500
@app.route('/api/proxy/building/<building_id>', methods=['POST'])
def proxy_save_building_data(building_id):
    try:
        data = request.json
        response = api_post(f"/api/building-data", json_data=data)
        if not response or response.status_code not in (200, 201, 204):
            return jsonify({
                "success": False,
                "error": f"Failed to save building data: {response.text if response else 'No response'}"
            }), 400
        result = {"success": True, "message": "Building data saved successfully"}
        if response.content:
            try:
                response_data = response.json()
                if "id_mapping" in response_data:
                    result["id_mapping"] = response_data["id_mapping"]
            except:
                pass
        return jsonify(result)
    except Exception as e:
        logger.error(f"Error saving building data: {str(e)}", exc_info=True)
        return jsonify({
            "success": False,
            "error": f"Server error: {str(e)}"
        }), 500
def api_get(endpoint, params=None):
    headers = {}
    if session.get('auth_token'):
        headers['Authorization'] = f"Bearer {session.get('auth_token')}"
    url = f"{API_BASE_URL}{endpoint}"
    logger.debug(f"API GET request to {url} with params: {json.dumps(params) if params else 'None'}")
    try:
        logger.debug(f"Headers: {json.dumps(headers)}")
        response = requests.get(url, params=params, headers=headers)
        logger.debug(f"API GET response: Status {response.status_code}")
        if response.status_code != 200:
            logger.warning(f"API GET error: {response.status_code} - {response.text}")
        else:
            try:
                response_json = response.json()
                logger.debug(f"API GET response JSON (shortened): {json.dumps(response_json)[:1000]}")
                if len(json.dumps(response_json)) > 1000:
                    logger.debug("Response JSON truncated, full data available in response object")
            except Exception as json_error:
                logger.debug(f"Could not parse response as JSON: {str(json_error)}")
                logger.debug(f"Response text (shortened): {response.text[:500]}")
        return response
    except Exception as e:
        logger.error(f"API GET exception: {str(e)}", exc_info=True)
        return None
def api_post(endpoint, data=None, json_data=None):
    headers = {}
    if session.get('auth_token'):
        headers['Authorization'] = f"Bearer {session.get('auth_token')}"
    url = f"{API_BASE_URL}{endpoint}"
    logger.debug(f"API POST request to {url}")
    if data:
        logger.debug(f"POST form data: {json.dumps(data)}")
    if json_data:
        safe_json = json_data.copy() if json_data else {}
        if 'password' in safe_json:
            safe_json['password'] = '********'
        logger.debug(f"POST json data: {json.dumps(safe_json, indent=2)}")
    try:
        logger.debug(f"Headers: {json.dumps(headers)}")
        response = requests.post(url, data=data, json=json_data, headers=headers)
        logger.debug(f"API POST response: Status {response.status_code}")
        if response.status_code not in (200, 201, 204):
            logger.warning(f"API POST error: {response.status_code} - {response.text}")
        else:
            try:
                if response.content:
                    response_json = response.json()
                    logger.debug(f"API POST response JSON (shortened): {json.dumps(response_json)[:1000]}")
                    if len(json.dumps(response_json)) > 1000:
                        logger.debug("Response JSON truncated, full data available in response object")
                else:
                    logger.debug("API POST response: No content")
            except Exception as json_error:
                logger.debug(f"Could not parse response as JSON: {str(json_error)}")
                logger.debug(f"Response text (shortened): {response.text[:500]}")
        return response
    except Exception as e:
        logger.error(f"API POST exception: {str(e)}", exc_info=True)
        return None
def api_put(endpoint, data=None, json_data=None):
    headers = {}
    if session.get('auth_token'):
        headers['Authorization'] = f"Bearer {session.get('auth_token')}"
    url = f"{API_BASE_URL}{endpoint}"
    logger.debug(f"API PUT request to {url}")
    if data:
        logger.debug(f"PUT form data: {json.dumps(data)}")
    if json_data:
        safe_json = json_data.copy() if json_data else {}
        if 'password' in safe_json:
            safe_json['password'] = '********'
        logger.debug(f"PUT json data: {json.dumps(safe_json, indent=2)}")
    try:
        logger.debug(f"Headers: {json.dumps(headers)}")
        response = requests.put(url, data=data, json=json_data, headers=headers)
        logger.debug(f"API PUT response: Status {response.status_code}")
        if response.status_code != 200:
            logger.warning(f"API PUT error: {response.status_code} - {response.text}")
        else:
            try:
                if response.content:
                    response_json = response.json()
                    logger.debug(f"API PUT response JSON (shortened): {json.dumps(response_json)[:1000]}")
                    if len(json.dumps(response_json)) > 1000:
                        logger.debug("Response JSON truncated, full data available in response object")
                else:
                    logger.debug("API PUT response: No content")
            except Exception as json_error:
                logger.debug(f"Could not parse response as JSON: {str(json_error)}")
                logger.debug(f"Response text (shortened): {response.text[:500]}")
        return response
    except Exception as e:
        logger.error(f"API PUT exception: {str(e)}", exc_info=True)
        return None
def api_delete(endpoint):
    headers = {}
    if session.get('auth_token'):
        headers['Authorization'] = f"Bearer {session.get('auth_token')}"
    url = f"{API_BASE_URL}{endpoint}"
    logger.debug(f"API DELETE request to {url}")
    try:
        logger.debug(f"Headers: {json.dumps(headers)}")
        response = requests.delete(url, headers=headers)
        logger.debug(f"API DELETE response: Status {response.status_code}")
        if response.status_code != 204:
            logger.warning(f"API DELETE error: {response.status_code} - {response.text}")
        return response
    except Exception as e:
        logger.error(f"API DELETE exception: {str(e)}", exc_info=True)
        return None
def hash_password(password):
    return hashlib.sha256(password.encode()).hexdigest()
def login_user(email, password):
    logger.info(f"Login attempt for email: {email}")
    try:
        response = api_post("/api/auth/login", json_data={
            "email": email,
            "password": password
        })
        if response and response.status_code == 200:
            data = response.json()
            session['auth_token'] = data.get('token')
            session['logged_in'] = True
            session['email'] = data.get('user', {}).get('email')
            session['name'] = data.get('user', {}).get('name')
            session['user_id'] = data.get('user', {}).get('id')
            logger.info(f"Login successful for user: {email}")
            logger.debug(f"Session data set: logged_in={session.get('logged_in')}, email={session.get('email')}")
            return True, "Login successful!"
        else:
            error_msg = "Invalid email or password."
            if response:
                error_data = response.json() if response.content else {}
                error_msg = error_data.get('message', error_msg)
            logger.warning(f"Login failed for user {email}: {error_msg}")
            logger.debug(f"Response status: {response.status_code if response else 'No response'}")
            return False, error_msg
    except Exception as e:
        logger.error(f"Login exception for user {email}: {str(e)}", exc_info=True)
        return False, "Error during login. Please try again."
@app.route("/", methods=["GET", "POST"])
def home():
    login_message = ""
    if request.method == "POST":
        email = request.form.get("email", "")
        password = request.form.get("password", "")
        logger.info(f"Login form submitted for email: {email}")
        success, login_message = login_user(email, password)
        if success:
            logger.info(f"Redirecting user {email} to admin home after successful login")
            return redirect(url_for("admin_home"))
    return render_template("index.html", message=login_message)
@app.route("/logout", methods=["GET"])
def logout():
    logger.info(f"Logout requested for user: {session.get('email')}")
    api_post("/api/auth/logout")  
    session.pop('logged_in', None)
    session.pop('email', None)
    session.pop('name', None)
    session.pop('user_id', None)
    session.pop('auth_token', None)
    session.clear()
    logger.info("User logged out successfully")
    return redirect(url_for("home"))
@app.route("/signup", methods=["GET", "POST"])
def signup():
    if session.get('logged_in'):
        logger.info(f"Already logged in user {session.get('email')} attempted to access signup page")
        return redirect(url_for("admin_homepage"))
    signup_message = ""
    if request.method == "POST":
        name = request.form.get("name", "")
        email = request.form.get("email", "")
        password = request.form.get("password", "")
        confirm_password = request.form.get("confirm_password", "")
        logger.info(f"Signup form submitted for email: {email}")
        if not name or not email or not password or not confirm_password:
            signup_message = "All fields are required!"
            logger.warning(f"Signup validation failed: {signup_message}")
            return render_template("signup.html", message=signup_message)
        if password != confirm_password:
            signup_message = "Passwords do not match!"
            logger.warning(f"Signup validation failed: {signup_message}")
            return render_template("signup.html", message=signup_message)
        logger.info(f"Attempting to register new user: {email}")
        response = api_post("/api/auth/signup", json_data={
            "name": name,
            "email": email,
            "password": password
        })
        if response and response.status_code in (200, 201):
            logger.info(f"User registration successful for email: {email}")
            token = None
            user_id = None
            try:
                response_data = response.json()
                token = response_data.get('token')
                user_id = response_data.get('user', {}).get('id')
            except:
                pass
            if not token:
                login_response = api_post("/api/auth/login", json_data={
                    "email": email,
                    "password": password
                })
                if login_response and login_response.status_code == 200:
                    login_data = login_response.json()
                    token = login_data.get('token')
                    user_id = login_data.get('user', {}).get('id')
                else:
                    flash("Signup successful! Please log in.")
                    return redirect(url_for("home"))
            session['auth_token'] = token
            session['logged_in'] = True
            session['email'] = email
            session['name'] = name
            session['user_id'] = user_id
            logger.info(f"Auto-login successful for new user: {email}")
            return redirect(url_for("admin_homepage"))
        else:
            error_data = {}
            if response and response.content:
                try:
                    error_data = response.json()
                except json.JSONDecodeError:
                    logger.error(f"Could not parse JSON response: {response.text}")
                    error_data = {"message": "Registration failed with invalid response from server"}
            signup_message = error_data.get('message', "Registration failed. Please try again.")
            logger.error(f"User registration failed for {email}: {signup_message}")
            logger.debug(f"API response: {response.status_code if response else 'No response'} - {error_data}")
            return render_template("signup.html", message=signup_message)
    return render_template("signup.html", message=signup_message)
@app.route('/login/google')
def google_login():
    session.pop('logged_in', None)
    session.pop('email', None)
    session.pop('name', None)
    session.pop('user_id', None)
    session.pop('oauth_state', None)
    flow = {
        'auth_uri': 'https://accounts.google.com/o/oauth2/auth',
        'token_uri': 'https://oauth2.googleapis.com/token',
        'client_id': os.getenv('GOOGLE_CLIENT_ID'),
        'client_secret': os.getenv('GOOGLE_CLIENT_SECRET'),
        'redirect_uri': url_for('google_auth', _external=True),
        'scope': 'openid email profile'
    }
    session['oauth_state'] = secrets.token_urlsafe(16)
    auth_url = (f"{flow['auth_uri']}?"
                f"client_id={flow['client_id']}&"
                f"redirect_uri={flow['redirect_uri']}&"
                f"scope={flow['scope']}&"
                f"response_type=code&"
                f"prompt=consent&"
                f"state={session['oauth_state']}")
    return redirect(auth_url)
@app.route("/api/protected/tenants", methods=["GET"])
def get_all_tenants():
    if not session.get('logged_in'):
        return jsonify({"error": "Authentication required"}), 401
    user_id = session.get('user_id')
    if not user_id:
        return jsonify({"error": "User ID is required"}), 400
    logger.info(f"Getting tenants for user ID: {user_id}")
    response = api_get("/api/protected/tenants")
    if response and response.status_code == 200:
        tenants = response.json()
        logger.info(f"Retrieved {len(tenants)} tenants for user {user_id}")
        return jsonify(tenants), 200
    else:
        error_msg = "Failed to retrieve tenants"
        logger.error(f"Failed to retrieve tenants: {response.status_code if response else 'No response'}")
        return jsonify({"error": error_msg}), response.status_code if response else 500
@app.route("/api/protected/tenants/<permission_id>", methods=["DELETE"])
def revoke_tenant_permission(permission_id):
    if not session.get('logged_in'):
        return jsonify({"error": "Authentication required"}), 401
    if not permission_id:
        return jsonify({"error": "Permission ID is required"}), 400
    logger.info(f"Revoking tenant permission ID: {permission_id}")
    response = api_delete(f"/api/protected/tenants/{permission_id}")
    if response and response.status_code in (200, 204):
        logger.info(f"Successfully revoked tenant permission {permission_id}")
        return jsonify({
            "status": "success",
            "message": "Tenant permission successfully revoked"
        }), 200
    else:
        error_msg = "Failed to revoke tenant permission"
        if response and response.content:
            try:
                error_data = response.json()
                error_msg = error_data.get('message', error_msg)
            except:
                pass
        logger.error(f"Failed to revoke tenant permission: {response.status_code if response else 'No response'}")
        return jsonify({
            "status": "error",
            "message": error_msg
        }), response.status_code if response else 500
@app.route('/login/google/callback')
def google_auth():
    import requests
    state = request.args.get('state', '')
    if state != session.get('oauth_state'):
        return "State verification failed", 403
    code = request.args.get('code')
    if not code:
        return "Authorization code not received", 400
    token_params = {
        'code': code,
        'client_id': os.getenv('GOOGLE_CLIENT_ID'),
        'client_secret': os.getenv('GOOGLE_CLIENT_SECRET'),
        'redirect_uri': url_for('google_auth', _external=True),
        'grant_type': 'authorization_code'
    }
    try:
        r = requests.post('https://oauth2.googleapis.com/token', data=token_params)
        token_data = r.json()
        if 'error' in token_data:
            return f"Token error: {token_data['error']}", 400
        headers = {'Authorization': f"Bearer {token_data['access_token']}"}
        userinfo_response = requests.get('https://www.googleapis.com/oauth2/v3/userinfo', headers=headers)
        user_info = userinfo_response.json()
        google_id = user_info.get('sub')  
        email = user_info.get('email')
        name = user_info.get('name')
        if not email:
            return "Could not get user email from Google", 400
        api_response = api_post("/api/auth/google/login", json_data={
            'email': user_info.get('email'),
            'name': user_info.get('name', ''),
            'picture': user_info.get('picture', ''),
            'google_id': user_info.get('sub')  
        })
        if api_response and api_response.status_code == 200:
            data = api_response.json()
            session['auth_token'] = data.get('token')
            session['logged_in'] = True
            session['email'] = data.get('user', {}).get('email')
            session['name'] = data.get('user', {}).get('name')
            session['user_id'] = data.get('user', {}).get('id')
            logger.info(f"Google login successful for user: {session.get('email')}")
            logger.debug(f"Session data: {session}")
            return redirect(url_for('admin_home'))
        else:
            error_msg = "Authentication failed"
            if api_response and api_response.content:
                try:
                    error_data = api_response.json()
                    error_msg = error_data.get('message', error_msg)
                except:
                    pass
            logger.error(f"Google auth API error: {error_msg}")
            logger.debug(f"API response: {api_response.status_code if api_response else 'No response'}")
            flash(f"Authentication error: {error_msg}")
            return redirect(url_for('home'))
    except Exception as e:
        logger.error(f"OAuth error: {str(e)}", exc_info=True)
        flash("An error occurred during authentication. Please try again.")
        return redirect(url_for('home'))
def get_user_id():
    user_id = session.get('user_id')
    if not user_id:
        return redirect(url_for('home'))
    return user_id
@app.route("/api/protected/rooms/accessible", methods=["GET"])
def get_accessible_rooms():
    if not session.get('logged_in'):
        return jsonify({"error": "Authentication required"}), 401
    user_id = request.args.get('userId') or session.get('user_id')
    search_term = request.args.get('search', '')
    if not user_id:
        return jsonify({"error": "User ID is required"}), 400
    response = api_get(f"/api/protected/rooms/accessible?userId={user_id}&search={search_term}")
    if response and response.status_code == 200:
        return response.json(), 200
    else:
        return jsonify({"error": "Failed to retrieve accessible rooms"}), response.status_code if response else 500
@app.route("/get_buildings", methods=["GET"])
def get_buildings():
    logger.info("Get buildings request")
    user_id = get_user_id()
    response = api_get("/api/protected/buildings/accessible",params={"userId": user_id})
    if response and response.status_code == 200:
        buildings_data = response.json()
        buildings = []
        for building in buildings_data:
            buildings.append((
                str(building['id']), 
                building['name'], 
                building['floorCount'],
                building['description'],
                building['adminContacts'],
                building.get('buildingLogs', [])
            ))
        logger.info(f"Retrieved {len(buildings)} buildings")
        return jsonify({"buildings": buildings}), 200
    else:
        logger.error(f"Failed to retrieve buildings: {response.status_code if response else 'No response'}")
        return jsonify({"error": "Failed to retrieve buildings"}), response.status_code if response else 500
@app.route("/building-logs/building/<building_id>", methods=["GET"])
def get_building_logs(building_id):
    if not session.get('logged_in'):
        return jsonify({"error": "Authentication required"}), 401
    logger.info(f"Getting logs for building {building_id}")
    response = api_get(f"/api/building-logs/building/{building_id}")
    if response and response.status_code == 200:
        logs = response.json()
        logger.info(f"Retrieved {len(logs)} logs for building {building_id}")
        return jsonify(logs), 200
    else:
        error_msg = "Failed to retrieve building logs"
        logger.error(f"Building log retrieval failed: {error_msg}")
        return jsonify({"error": error_msg}), response.status_code if response else 500
@app.route("/create_building", methods=["POST"])
def create_building():
    building_name = request.form.get("building_name", "")
    floors = int(request.form.get("floors", 3))  
    description = request.form.get("description", "")
    logger.info(f"Create building request: {building_name}, description: {description}")
    if not building_name:
        logger.warning("Invalid building creation - missing building name")
        return jsonify({"error": "Building name is required"}), 400
    building_response = api_post("/api/buildings", json_data={
        "name": building_name,
        "description": description
    })
    if not building_response or building_response.status_code not in (200, 201):
        logger.error(f"Failed to create building: {building_response.status_code if building_response else 'No response'}")
        return jsonify({"error": "Failed to create building"}), building_response.status_code if building_response else 500
    building_data = building_response.json()
    building_id = building_data['id']
    logger.info(f"Building created with ID: {building_id}")
    for floor_num in range(1, floors + 1):
        floor_response = api_post("/api/floors", json_data={
            "buildingId": building_id,
            "floorNumber": floor_num,
            "displayName": f"Floor {floor_num}",
            "accessible": True
        })
        if not floor_response or floor_response.status_code not in (200, 201):
            logger.warning(f"Failed to create floor {floor_num} for building {building_id}")
        else:
            logger.info(f"Created floor {floor_num} for building {building_id}")
    logger.info(f"Redirecting to draw page for building {building_id}")
    return redirect(url_for('draw', building_id=building_id, floors=floors))
@app.route("/update_building", methods=["POST"])
def update_building():
    building_id = request.form.get("building_id")
    building_name = request.form.get("building_name")
    floors = request.form.get("floors")
    description = request.form.get("description", "")
    logger.info(f"Update building request for building ID: {building_id}")
    if not building_id or not building_name or not floors:
        logger.warning("Invalid building update params")
        return jsonify({"error": "All fields are required"}), 400
    building_response = api_put(f"/api/buildings/{building_id}", json_data={
        "name": building_name,
        "description": description
    })
    if not building_response or building_response.status_code != 200:
        logger.error(f"Failed to update building: {building_response.status_code if building_response else 'No response'}")
        return jsonify({"error": "Failed to update building"}), building_response.status_code if building_response else 500
    logger.info(f"Building {building_id} updated successfully")
    floors_response = api_get(f"/api/floors/building/{building_id}")
    if floors_response and floors_response.status_code == 200:
        existing_floors = floors_response.json()
        floors_needed = int(floors)
        logger.info(f"Building has {len(existing_floors)} floors, needs {floors_needed}")
        if len(existing_floors) < floors_needed:
            for floor_num in range(len(existing_floors) + 1, floors_needed + 1):
                floor_response = api_post("/api/floors", json_data={
                    "buildingId": building_id,
                    "floorNumber": floor_num,
                    "displayName": f"Floor {floor_num}",
                    "accessible": True
                })
                if floor_response and floor_response.status_code in (200, 201):
                    logger.info(f"Created additional floor {floor_num}")
                else:
                    logger.warning(f"Failed to create additional floor {floor_num}")
    log_response = api_post("/api/building-logs", json_data={
        "userId": session.get('user_id'),
        "buildingId": building_id,
        "actionType": "UPDATE_BUILDING",
        "buildingName": building_name,
        "details": f"Building with id {building_id} has changed its details. Name: {building_name} Description: {description} floors: {floors}"
    })
    return jsonify({"message": "Building updated successfully!"}), 200
@app.route("/delete_building", methods=["POST"])
def delete_building():
    building_id = request.form.get("building_id")
    logger.info(f"Delete building request for building ID: {building_id}")
    if not building_id:
        logger.warning("Building ID not provided for deletion")
        return jsonify({"error": "Building ID is required"}), 400
    response = api_delete(f"/api/buildings/{building_id}")
    if response and response.status_code == 204:
        logger.info(f"Building {building_id} deleted successfully")
        return jsonify({"message": "Building deleted successfully!"}), 200
    else:
        logger.error(f"Failed to delete building: {response.status_code if response else 'No response'}")
        return jsonify({"error": "Failed to delete building"}), response.status_code if response else 500
@app.route("/api/protected/request-access", methods=["POST"])
def request_access():
    if not session.get('logged_in'):
        return jsonify({"error": "Authentication required"}), 401
    data = request.get_json()
    user_id = data.get('userId') or session.get('user_id')
    building_id = data.get('buildingId')
    room_id = data.get('roomId')
    access_type = data.get('accessType', 'standard')
    logger.info(f"Access request from user {user_id} for building {building_id}, room {room_id}, type {access_type}")
    response = api_post("/api/protected/request-access", json_data={
        "userId": user_id,
        "buildingId": building_id,
        "roomId": room_id,
        "accessType": access_type
    })
    if response and response.status_code in (200, 201):
        return jsonify({"success": True, "message": "Access request submitted successfully"}), 200
    else:
        error_msg = "Failed to submit access request"
        if response and response.content:
            try:
                error_data = response.json()
                error_msg = error_data.get('message', error_msg)
            except:
                pass
        logger.error(f"Access request failed: {error_msg}")
        return jsonify({"success": False, "message": error_msg}), 400
@app.route("/api/protected/process-request", methods=["POST"])
def process_request():
    if not session.get('logged_in'):
        return jsonify({"error": "Authentication required"}), 401
    data = request.get_json()
    request_id = data.get('requestId')
    approved = data.get('approved')
    processor_id = data.get('processorId') or session.get('user_id')
    if not request_id or approved is None or not processor_id:
        return jsonify({"error": "Request ID, approval status, and processor ID are required"}), 400
    logger.info(f"Processing access request {request_id}: approved={approved} by user {processor_id}")
    response = api_post("/api/protected/process-request", json_data={
        "requestId": request_id,
        "approved": approved,
    })
    if response and response.status_code == 200:
        return jsonify({"success": True, "message": "Request processed successfully"}), 200
    else:
        error_msg = "Failed to process request"
        if response and response.content:
            try:
                error_data = response.json()
                error_msg = error_data.get('message', error_msg)
            except:
                pass
        logger.error(f"Process request failed: {error_msg}")
        return jsonify({"success": False, "message": error_msg}), 400
@app.route("/api/protected/grant-access-through-code", methods=["POST"])
def grant_access_through_code():
    if not session.get('logged_in'):
        return jsonify({"error": "Authentication required"}), 401
    data = request.get_json()
    user_id = data.get('userId') or session.get('user_id')
    code = data.get('code')
    if not user_id or not code:
        return jsonify({"error": "User ID and access code are required"}), 400
    logger.info(f"Granting access to user {user_id} using code {code}")
    response = api_post("/api/protected/grant-access-through-code", json_data={
        "userId": user_id,
        "code": code
    })
    if response and response.status_code == 200:
        return jsonify({"success": True, "message": "Access granted successfully"}), 200
    else:
        error_msg = "Failed to grant access"
        if response and response.content:
            try:
                error_data = response.json()
                error_msg = error_data.get('message', error_msg)
            except:
                pass
        logger.error(f"Grant access failed: {error_msg}")
        return jsonify({"success": False, "message": error_msg}), 400
@app.route('/save_drawing', methods=['POST'])
def update_floor_name():
    data = request.get_json()
    floor_id = data.get('floor_id')
    new_name = data.get('new_name')
    building_id = data.get('building_id')
    building_name = data.get('building_name')
    current_name = data.get('current_name')
    logger.info(f"Updating floor {floor_id} name to {new_name}")
    if not floor_id or not new_name:
        logger.warning("Missing floor ID or new name")
        return jsonify({"error": "Floor ID and new name are required"}), 400
    response = api_put(f"/api/floors/{floor_id}/name/{new_name}")
    if response and response.status_code == 200:
        logger.info(f"Successfully updated floor {floor_id} name")
        log_response = api_post("/api/building-logs", json_data={
        "userId": session.get('user_id'),
        "buildingId": building_id,
        "actionType": "UPDATE_BUILDING",
        "buildingName": building_name,
        "floorName": new_name,
        "details": f"Floor name of floor {current_name}"
    })
        return jsonify({"success": True, "message": "Floor name updated successfully"}), 200
    else:
        logger.error(f"Failed to update floor {floor_id} name")
        return jsonify({"error": "Failed to update floor name"}), response.status_code if response else 500
def request_building_access():
    data = request.get_json()
    building_id = data.get("building_id")
    user_id = session.get("user_id")
    access_type = data.get("access_type", "standard")
    logger.info(f"Building access request for building ID: {building_id} by user ID: {user_id}")
    if not building_id or not user_id:
        logger.warning("Building ID or user ID not provided for access request")
        return jsonify({"success": False, "message": "Building ID and user ID are required"}), 400
    try:
        response = api_post("/api/protected/request-access", json_data={
            "userId": user_id,
            "buildingId": building_id,
            "accessType": access_type
        })
        if response and response.status_code in (200, 201):
            logger.info(f"Building access request successful for building {building_id}")
            return jsonify({"success": True, "message": "Building access request sent successfully!"}), 200
        else:
            error_msg = "Failed to send building access request"
            if response and response.content:
                try:
                    error_data = response.json()
                    error_msg = error_data.get('message', error_msg)
                except:
                    pass
            logger.error(f"Failed to send building access request: {response.status_code if response else 'No response'}")
            return jsonify({"success": False, "message": error_msg}), response.status_code if response else 500
    except Exception as e:
        logger.error(f"Error requesting building access: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": f"An error occurred: {str(e)}"}), 500
@app.route("/api/buildings/all", methods=["GET"])
def get_all_buildings():
    logger.info("Get all buildings request for access request modal")
    try:
        response = api_get("/api/buildings")
        if response and response.status_code == 200:
            buildings = response.json()
            logger.info(f"Retrieved {len(buildings)} buildings for access request modal")
            return jsonify({"buildings": buildings}), 200
        else:
            logger.error(f"Failed to retrieve buildings: {response.status_code if response else 'No response'}")
            return jsonify({"error": "Failed to retrieve buildings"}), response.status_code if response else 500
    except Exception as e:
        logger.error(f"Error getting all buildings: {str(e)}", exc_info=True)
        return jsonify({"error": f"Failed to get buildings: {str(e)}"}), 500
@app.route("/api/buildings/<building_id>/admins", methods=["GET"])
def get_building_admins(building_id):
    logger.info(f"Get admins request for building ID: {building_id}")
    try:
        response = api_get(f"/api/buildings/{building_id}/admins")
        if response and response.status_code == 200:
            admins = response.json()
            logger.info(f"Retrieved {len(admins)} admins for building {building_id}")
            return jsonify({"admins": admins}), 200
        else:
            logger.error(f"Failed to retrieve building admins: {response.status_code if response else 'No response'}")
            return jsonify({"error": "Failed to retrieve building administrators"}), response.status_code if response else 500
    except Exception as e:
        logger.error(f"Error getting building admins: {str(e)}", exc_info=True)
        return jsonify({"error": f"Failed to get building administrators: {str(e)}"}), 500
@app.route('/forgot-password', methods=['GET', 'POST'])
def forgot_password():
    if request.method == 'POST':
        email = request.form.get('email')
        logger.info(f"Password reset requested for email: {email}")
        response = api_post("/api/auth/forgot-password", json_data={"email": email})
        if response and response.status_code == 200:
            data = response.json()
            reset_token = data.get('resetToken')
            reset_url = url_for('reset_password', token=reset_token, _external=True)
            msg = Message("Password Reset Request", recipients=[email])
            msg.body = f"You requested a password reset for your JANUS account.\n\n"
            msg.body += f"Click the following link to reset your password: {reset_url}\n\n"
            msg.body += f"This link will expire in 3 hours.\n\n"
            msg.body += f"If you did not request this reset, please ignore this email."
            try:
                mail.send(msg)
                logger.info(f"Password reset link sent to {email}")
                flash("Password reset link has been sent to your email.")
            except Exception as e:
                logger.error(f"Failed to send email to {email}: {str(e)}", exc_info=True)
                flash("Failed to send reset email. Please try again later.")
        else:
            error_msg = "Email not found or failed to send reset email."
            if response and response.content:
                try:
                    error_data = response.json()
                    error_msg = error_data.get('message', error_msg)
                except:
                    pass
            logger.warning(f"Failed to send password reset email to {email}: {response.status_code if response else 'No response'}")
            flash(error_msg)
        return render_template('forgot_password.html')
    return render_template('forgot_password.html')
@app.route('/reset-password/<token>', methods=['GET', 'POST'])
def reset_password(token):
    logger.info(f"Password reset page accessed with token")
    verify_response = api_post("/api/auth/verify-reset", json_data={"resetToken": token})
    if not verify_response or verify_response.status_code != 200:
        logger.warning(f"Invalid or expired password reset token")
        flash("Invalid or expired token.")
        return redirect(url_for('home'))
    if request.method == 'POST':
        password = request.form.get('password')
        confirm_password = request.form.get('confirm_password')
        if password != confirm_password:
            logger.warning("Password reset failed: passwords do not match")
            flash("Passwords do not match.")
            return render_template('reset_password.html', token=token)
        reset_response = api_post("/api/auth/change-password", json_data={
            "resetToken": token,
            "newPassword": password
        })
        if reset_response and reset_response.status_code == 200:
            logger.info("Password reset successful")
            flash("Password has been reset successfully. Please log in.")
            return redirect(url_for('home'))
        else:
            error_msg = "Failed to reset password. Please try again."
            if reset_response and reset_response.content:
                try:
                    error_data = reset_response.json()
                    error_msg = error_data.get('message', error_msg)
                except:
                    pass
            logger.error(f"Failed to reset password: {reset_response.status_code if reset_response else 'No response'}")
            flash(error_msg)
            return render_template('reset_password.html', token=token)
    return render_template('reset_password.html', token=token)
@app.route("/admin", methods=["GET"])
def admin_home():
    logger.info(f"Admin home page requested by user: {session.get('email')}")
    if not session.get('logged_in'):
        logger.warning("Unauthenticated user attempted to access admin page")
        return redirect(url_for("home"))
    logger.info(f"Rendering admin homepage for user: {session.get('email')}")
    return render_template("admin_homepage.html", email=session.get('email'))
@app.route("/draw", methods=["GET"])
def draw():
    if not session.get('logged_in'):
        logger.warning("Unauthenticated user attempted to access draw page")
        return redirect(url_for("home"))
    building_id = request.args.get("building_id")
    floors = request.args.get("floors")
    logger.info(f"Draw page requested for building {building_id} floors by user {session.get('email')}")
    return render_template("draw.html", building_id=building_id)
@app.route("/drawnew", methods=["GET"])
def drawnew():
    return render_template("newdraw.html",)
@app.route('/building_layout')
def building_layout():
    logger.info("Building layout page requested")
    return render_template('building_layout.html')
@app.route('/check_session', methods=['GET'])
def check_session():
    logged_in = session.get('logged_in', False)
    logger.debug(f"Session check: logged_in={logged_in}, email={session.get('email')}")
    return jsonify({
        "logged_in": logged_in,
        "email": session.get('email'),
        "name": session.get('name')
    })
@app.route('/debug/session', methods=['GET'])
def debug_session():
    if app.debug:
        logger.debug(f"Debug session info requested for session: {session}")
        return jsonify({
            "logged_in": session.get('logged_in', False),
            "email": session.get('email'),
            "name": session.get('name'),
            "user_id": session.get('user_id'),
            "session_keys": list(session.keys())
        })
    else:
        return jsonify({"error": "This endpoint is only available in debug mode"}), 403
@app.before_request
def check_authentication():
    if request.endpoint and request.endpoint not in ('static', 'home', 'login_user', 'logout', 'signup', 
                                                  'google_login', 'google_auth', 'forgot_password', 
                                                  'reset_password', 'check_session', 'debug_session'):
        if not session.get('logged_in') and request.path != '/':
            logger.warning(f"Unauthenticated access attempt to {request.path}")
            if request.path.startswith('/api/') or request.is_xhr:
                return jsonify({"error": "Authentication required"}), 401
    if logger.isEnabledFor(logging.DEBUG):
        logger.debug(f"Request to {request.path}: session logged_in={session.get('logged_in')}, email={session.get('email')}")
@app.route("/admin_homepage")
def admin_homepage():
    return render_template("admin_homepage.html")     
@app.route("/get_rooms", methods=["GET"])
def get_rooms():
    building_id = request.args.get("building_id")
    logger.info(f"Get rooms request for building ID: {building_id}")
    if not building_id:
        logger.warning("Building ID not provided for get rooms")
        return jsonify({"error": "Building ID is required"}), 400
    try:
        response = api_get(f"/api/rooms/building/{building_id}")
        if response and response.status_code == 200:
            rooms = response.json()
            logger.info(f"Retrieved {len(rooms)} rooms for building {building_id}")
            return jsonify({"rooms": rooms}), 200
        else:
            logger.error(f"Failed to retrieve rooms: {response.status_code if response else 'No response'}")
            return jsonify({"error": "Failed to retrieve rooms"}), response.status_code if response else 500
    except Exception as e:
        logger.error(f"Error fetching rooms: {str(e)}", exc_info=True)
        return jsonify({"error": f"Failed to fetch rooms: {str(e)}"}), 500
@app.route("/request_room_access", methods=["POST"])
def request_room_access():
    data = request.get_json()
    room_id = data.get("room_id")
    user_id = session.get("user_id")
    logger.info(f"Room access request for room ID: {room_id} by user ID: {user_id}")
    if not room_id or not user_id:
        logger.warning("Room ID or user ID not provided for access request")
        return jsonify({"error": "Room ID and user ID are required"}), 400
    try:
        response = api_post("/api/rooms/request-access", json_data={
            "room_id": room_id,
            "user_id": user_id
        })
        if response and response.status_code == 200:
            logger.info(f"Room access request successful for room {room_id}")
            return jsonify({"message": "Room access request sent successfully!"}), 200
        else:
            logger.error(f"Failed to send room access request: {response.status_code if response else 'No response'}")
            return jsonify({"error": "Failed to send room access request"}), response.status_code if response else 500
    except Exception as e:
        logger.error(f"Error requesting room access: {str(e)}", exc_info=True)
        return jsonify({"error": f"Failed to request room access: {str(e)}"}), 500
@app.route("/search_rooms", methods=["GET"])
def search_rooms():
    query = request.args.get("query")
    logger.info(f"Search rooms request with query: {query}")
    if not query:
        logger.warning("No search query provided")
        return jsonify({"error": "Search query is required"}), 400
    try:
        response = api_get("/api/rooms/search", params={"query": query})
        if response and response.status_code == 200:
            rooms = response.json()
            logger.info(f"Found {len(rooms)} rooms matching query: {query}")
            return jsonify({"rooms": rooms}), 200
        else:
            logger.error(f"Failed to search rooms: {response.status_code if response else 'No response'}")
            return jsonify({"error": "Failed to search rooms"}), response.status_code if response else 500
    except Exception as e:
        logger.error(f"Error searching rooms: {str(e)}", exc_info=True)
        return jsonify({"error": f"Failed to search rooms: {str(e)}"}), 500   
@app.route("/api/protected/user/my-requests", methods=["GET"])
def get_user_my_requests():
    if not session.get('logged_in'):
        return jsonify({"error": "Authentication required"}), 401
    response = api_get(f"/api/protected/user/my-requests")
    if response and response.status_code == 200:
        logger.info(f"Retrieved user access requests")
        return response.json(), 200
    else:
        error_msg = "Failed to retrieve user requests"
        logger.error(f"Failed to retrieve user requests: {response.status_code if response else 'No response'}")
        return jsonify({"error": error_msg}), response.status_code if response else 500
@app.route("/api/protected/admin/building-requests", methods=["GET"])
def get_admin_building_requests():
    if not session.get('logged_in'):
        return jsonify({"error": "Authentication required"}), 401
    response = api_get(f"/api/protected/admin/building-requests")
    if response and response.status_code == 200:
        logger.info(f"Retrieved admin building requests")
        return response.json(), 200
    else:
        error_msg = "Failed to retrieve admin building requests"
        logger.error(f"Failed to retrieve admin building requests: {response.status_code if response else 'No response'}")
        return jsonify({"error": error_msg}), response.status_code if response else 500
@app.route('/update_room/<room_id>', methods=['PUT'])
def update_room(room_id):
    if not session.get('logged_in'):
        logger.warning("Unauthorized attempt to update room")
        return jsonify(success=False, error="Authentication required"), 401
    try:
        data = request.get_json()
        logger.info(f"Updating room {room_id} with data: {json.dumps({k: v for k, v in data.items() if k != 'contactDetails'})}")
        room_data = {
            "name": data.get('name', ''),
            "description": data.get('description', ''),
            "contactDetails": data.get('contactDetails', ''),
            "roomType": data.get('roomType','')
        }
        response = api_put(f"/api/map-objects/{room_id}/update-room", json_data=room_data)
        if response and response.status_code == 200:
            logger.info(f"Successfully updated room {room_id}")
            log_response = api_post("/api/building-logs", json_data={
            "userId": session.get('user_id'),
            "buildingId": data.get('buildingId',''),
            "actionType": "UPDATE_BUILDING",
            "details": f"Updated room details of room {room_id} to name: {data.get('name', '')} description: {data.get('description', '')} contact: {data.get('contactDetails', '')} "
            })
            return jsonify(success=True, message="Room updated successfully")
        else:
            error_msg = "Failed to update room"
            if response and response.content:
                try:
                    error_data = response.json()
                    error_msg = error_data.get('message', error_msg)
                except:
                    pass
            logger.error(f"Failed to update room {room_id}: {response.status_code if response else 'No response'}")
            return jsonify(success=False, message=error_msg), 400
    except Exception as e:
        logger.error(f"Exception updating room {room_id}: {str(e)}", exc_info=True)
        return jsonify(success=False, error=f"An error occurred: {str(e)}"), 500
if __name__ == "__main__":
    logger.info("Application starting up")
    app.run(debug=True)