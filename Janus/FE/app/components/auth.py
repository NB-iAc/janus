import gradio as gr
import re
import requests
import os
from datetime import datetime

class AuthMiddleware:
    def __init__(self, api_base_url):
        """
        Initialize AuthMiddleware with API base URL
        
        :param api_base_url: Base URL for authentication API
        """
        self.api_base_url = api_base_url
        
    def validate_email(self, email):
        """
        Validate email format
        
        :param email: Email address to validate
        :return: True if email is valid, False otherwise
        """
        pattern = r'^[\w\.-]+@[\w\.-]+\.\w+$'
        return re.match(pattern, email) is not None
    
    def validate_password(self, password):
        """
        Validate password strength
        
        :param password: Password to validate
        :return: Tuple of (is_valid, message)
        """
        if len(password) < 8:
            return False, "Password must be at least 8 characters long"
        if not any(c.isdigit() for c in password):
            return False, "Password must contain at least one number"
        return True, "Password is valid"
    
    def login(email, password):
        if not email or not password:
            return "Please enter both email and password", gr.update(visible=True), gr.update(visible=False)
        
        if not validate_email(email):
            return "Please enter a valid email address", gr.update(visible=True), gr.update(visible=False)
        
        try:
            # API login endpoint
            response = requests.post(
                "
                json={
                    "email": email,
                    "password": password
                }
            )
            
            if response.status_code == 200:
                data = response.json()
                name = data.get('user', {}).get('name', 'User')
                return f"Welcome back, {name}!", gr.update(visible=False), gr.update(visible=True)
            else:
                error_msg = response.json().get('message', 'Invalid email or password')
                return error_msg, gr.update(visible=True), gr.update(visible=False)
        
        except requests.RequestException as e:
            return f"Login error: {str(e)}", gr.update(visible=True), gr.update(visible=False)

    def signup(name, email, password, confirm_password):
        # Validate inputs
        if not name or not email or not password or not confirm_password:
            return "All fields are required", gr.update(visible=True)
        
        if not validate_email(email):
            return "Please enter a valid email address", gr.update(visible=True)
        
        if password != confirm_password:
            return "Passwords do not match", gr.update(visible=True)
        
        is_valid, message = validate_password(password)
        if not is_valid:
            return message, gr.update(visible=True)
        
        try:
            # API signup endpoint
            response = requests.post(
                "
                json={
                    "name": name,
                     "email": email,
                    "password": password
                }
            )
            
            if response.status_code in (200, 201):
                return f"Welcome, {name}! Your account has been created.", gr.update(visible=False)
            else:
                error_msg = response.json().get('message', 'Registration failed')
                return error_msg, gr.update(visible=True)
        
        except requests.RequestException as e:
            return f"Signup error: {str(e)}", gr.update(visible=True)


def create_app(api_base_url='https://redacted.link'):
    # Initialize auth middleware
    auth_middleware = AuthMiddleware(api_base_url)
    
    # Gradio UI setup
    with gr.Blocks() as demo:
        gr.Markdown("# JANUS - Your Guide Within", elem_id="header")
        
        with gr.Row():
            # Left Column: Login
            with gr.Column(scale=1, elem_id="left-column"):
                gr.Markdown("### Login to Your Account")
                email_input = gr.Textbox(label="Email", placeholder="Enter your email")
                password_input = gr.Textbox(label="Password", type="password", placeholder="Enter your password")
                
                with gr.Row():
                    login_button = gr.Button("Login", variant="primary", elem_id="login-btn")
                    forgot_password = gr.Button("Forgot Password?", variant="secondary", elem_id="forgot-btn")
                
                login_output = gr.Textbox(label="Login Status", interactive=False)
                
                login_button.click(
                    auth_middleware.login, 
                    inputs=[email_input, password_input], 
                    outputs=[login_output, login_button, gr.Button("Welcome")]
                )
                
                gr.Markdown("---")
                
                with gr.Row():
                    gr.Button("Sign in with Google", elem_id="google-signin")
                    gr.Button("Sign in with Apple", elem_id="apple-signin")

            # Right Column: Sign Up
            with gr.Column(scale=1, elem_id="right-column"):
                gr.Markdown("### New Here?\nSign up to design dynamic indoor maps and explore new horizons!")
                name_input = gr.Textbox(label="Full Name", placeholder="Enter your name")
                signup_email_input = gr.Textbox(label="Email", placeholder="Enter your email")
                signup_password = gr.Textbox(label="Password", type="password", placeholder="Create a password (min 8 characters)")
                confirm_password = gr.Textbox(label="Confirm Password", type="password", placeholder="Confirm your password")
                
                signup_button = gr.Button("Create Account", variant="primary", elem_id="signup-btn")
                signup_output = gr.Textbox(label="Signup Status", interactive=False)
                
                signup_button.click(
                    auth_middleware.signup, 
                    inputs=[name_input, signup_email_input, signup_password, confirm_password], 
                    outputs=[signup_output]
                )
                
                gr.Markdown("---")
                
                with gr.Row():
                    gr.Button("Sign up with Google", elem_id="google-signup")
                    gr.Button("Sign up with Apple", elem_id="apple-signup")

        # Styling for the layout (same as before)
        css = """
        #header {
            text-align: center;
            margin-bottom: 20px;
            color: #333;
        }
        
        #left-column {
            background-color: #b9d8d8;
            padding: 24px;
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            margin-right: 10px;
        }
        
        #right-column {
            background-color: #fff9f2;
            padding: 24px;
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            margin-left: 10px;
        }
        
        #login-btn, #signup-btn {
            background-color: #2a9d8f;
            color: white;
        }
        
        #forgot-btn {
            background-color: transparent;
            color: #666;
            font-size: 0.9em;
        }
        
        #google-signin, #google-signup {
            background-color: #4285F4;
            color: white;
        }
        
        #apple-signin, #apple-signup {
            background-color: #000;
            color: white;
        }
        
        /* Responsive design */
        @media (max-width: 768px) {
            .gradio-row {
                flex-direction: column;
            }
            
            #left-column, #right-column {
                width: 100%;
                margin: 10px 0;
            }
        }
        """
        demo.css(css)
    
    return demo

if __name__ == "__main__":
    app = create_app()
    app.launch()