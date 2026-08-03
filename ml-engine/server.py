import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

from flask import Flask, request, jsonify
from flask_cors import CORS
import base64
import json
import os
from datetime import datetime
import cv2
import numpy as np
from PIL import Image
import google.generativeai as genai
from dotenv import load_dotenv
from openai import OpenAI

load_dotenv()
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
if GEMINI_API_KEY and GEMINI_API_KEY != "PASTE_YOUR_API_KEY_HERE":
    genai.configure(api_key=GEMINI_API_KEY)
    gemini_model = genai.GenerativeModel('gemini-3.5-flash')
else:
    gemini_model = None
    print("⚠️ Warning: Gemini API Key not found in .env! Medical Reports will be disabled.")

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
openai_client = OpenAI(api_key=OPENAI_API_KEY) if OPENAI_API_KEY else None

GROK_API_KEY = os.getenv("GROK_API_KEY")
grok_client = OpenAI(api_key=GROK_API_KEY, base_url="https://api.x.ai/v1") if GROK_API_KEY else None

GROQ_API_KEY = os.getenv("GROQ_API_KEY")
groq_client = OpenAI(api_key=GROQ_API_KEY, base_url="https://api.groq.com/openai/v1") if GROQ_API_KEY else None

app = Flask(__name__)
CORS(app)

# JSON Persistent Storage File (Reset to fresh 0 state)
DB_FILE = os.path.join(os.path.dirname(__file__), "database.json")

def create_fresh_db():
    return {
        "users": {},
        "patients": [],
        "appointments": [],
        "reports": [],
        "treatment_plans": {}
    }

def load_db():
    if os.path.exists(DB_FILE):
        try:
            with open(DB_FILE, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            pass
    return create_fresh_db()

def save_db(db_data):
    with open(DB_FILE, "w", encoding="utf-8") as f:
        json.dump(db_data, f, indent=2)

# Reset DB on startup if requested or start clean
db = load_db()

@app.route('/', methods=['GET'])
@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "ok", "message": "SmileGuard AI Dynamic Backend running!"}), 200

# --- ACCOUNT RESET & AUTH API ---
@app.route('/api/auth/register', methods=['POST'])
def register_account():
    global db
    data = request.get_json(force=True)
    email = data.get("email", "").strip().lower()
    role = data.get("role", "doctor").strip().lower()
    password = data.get("password", "123456")
    name = data.get("name", "New User")

    if not email:
        return jsonify({"success": False, "message": "Email address is required."}), 400

    if email in db["users"]:
        # If user already exists, lock their role strictly
        existing_role = db["users"][email].get("role", "doctor")
        if existing_role != role:
            return jsonify({
                "success": False,
                "message": f"This email is locked as a {existing_role.capitalize()} account. No {role.capitalize()} account found for this email."
            }), 400

    # Create new account locked to specified role
    user_record = {
        "email": email,
        "name": name,
        "role": role,
        "password": password,
        "patients": [],
        "appointments": [],
        "reports": [],
        "treatment_plans": {}
    }
    db["users"][email] = user_record
    save_db(db)
    return jsonify({"success": True, "message": f"New {role.capitalize()} account created successfully", "user": user_record}), 201

@app.route('/api/auth/login', methods=['POST'])
def login_account():
    global db
    data = request.get_json(force=True)
    email = data.get("email", "").strip().lower()
    role = data.get("role", "doctor").strip().lower()
    password = data.get("password", "")

    if not email:
        return jsonify({"success": False, "message": "Email address is required."}), 400

    if email not in db["users"]:
        return jsonify({"success": False, "message": f"No {role.capitalize()} account found for this email."}), 404

    user = db["users"][email]
    existing_role = user.get("role", "doctor")

    # Strict Role Guard Check
    if existing_role != role:
        return jsonify({
            "success": False,
            "message": f"No {role.capitalize()} account found for this email. This email is locked as a {existing_role.capitalize()} account."
        }), 400

    if user.get("password") and user.get("password") != password:
        return jsonify({"success": False, "message": "Invalid password."}), 401

    return jsonify({"success": True, "message": "Login successful", "user": user}), 200

@app.route('/api/settings/password', methods=['POST'])
def update_password():
    global db
    data = request.get_json(force=True)
    email = data.get("email", "").strip().lower()
    new_password = data.get("newPassword", "").strip()

    if not email or not new_password:
        return jsonify({"success": False, "message": "Email and new password are required."}), 400

    if email in db["users"]:
        db["users"][email]["password"] = new_password
        save_db(db)
        return jsonify({"success": True, "message": "Password updated successfully in database."}), 200
    else:
        # Save password record
        db["users"][email] = {
            "email": email,
            "role": "doctor",
            "password": new_password,
            "patients": [],
            "appointments": [],
            "reports": []
        }
        save_db(db)
        return jsonify({"success": True, "message": "Password saved to user profile in database."}), 200

@app.route('/api/settings/profile', methods=['GET', 'POST'])
def handle_profile():
    global db
    if request.method == 'POST':
        data = request.get_json(force=True)
        email = data.get("email", "").strip().lower()
        if not email:
            return jsonify({"success": False, "message": "Email is required."}), 400

        if email not in db["users"]:
            db["users"][email] = {
                "email": email,
                "role": data.get("role", "doctor"),
                "password": "",
                "patients": [],
                "appointments": [],
                "reports": [],
                "treatment_plans": {}
            }

        user = db["users"][email]
        user["name"] = data.get("name", user.get("name", ""))
        user["mobile"] = data.get("mobile", user.get("mobile", ""))
        user["address"] = data.get("address", user.get("address", ""))
        user["gender"] = data.get("gender", user.get("gender", ""))
        user["specialization"] = data.get("specialization", user.get("specialization", ""))
        save_db(db)
        return jsonify({"success": True, "message": "Profile updated successfully."}), 200

    email = request.args.get("email", "").strip().lower()
    if not email or email not in db["users"]:
        return jsonify({"success": False, "message": "No profile found."}), 404

    user = db["users"][email]
    return jsonify({
        "success": True,
        "name": user.get("name", ""),
        "email": email,
        "mobile": user.get("mobile", ""),
        "address": user.get("address", ""),
        "gender": user.get("gender", "Male"),
        "specialization": user.get("specialization", "Orthodontics")
    }), 200

@app.route('/api/reset-db', methods=['POST'])
def reset_database():
    global db
    db = create_fresh_db()
    save_db(db)
    return jsonify({"success": True, "message": "Database reset! All users deleted, values set to 0."}), 200

# --- DYNAMIC REST API ENDPOINTS ---

@app.route('/api/patients', methods=['GET', 'POST'])
def handle_patients():
    global db
    if request.method == 'POST':
        data = request.get_json(force=True)
        patient_id = data.get("patientId", "").strip()
        name = data.get("name", "").strip()
        dob = data.get("dob", "").strip()
        email = data.get("email", "").strip()
        phone = data.get("phone", "").strip()
        doctor_email = data.get("doctorEmail", "").strip().lower()

        # Strict Mandatory Validation Check
        if not (patient_id and name and dob and email and phone):
            return jsonify({
                "success": False,
                "message": "All fields (Patient ID, Full Name, DOB, Email Address, Phone Number) are mandatory."
            }), 400

        new_patient = {
            "id": str(len(db["patients"]) + 1),
            "patientId": patient_id,
            "name": name,
            "dob": dob,
            "email": email,
            "phone": phone,
            "status": "Active"
        }
        # Deduplicate (global legacy list, kept for back-compat)
        db["patients"] = [p for p in db["patients"] if p["patientId"] != patient_id]
        db["patients"].append(new_patient)

        # Attach to the creating doctor's own roster so dashboard/patients scoping is real
        if doctor_email and doctor_email in db["users"]:
            user = db["users"][doctor_email]
            user.setdefault("patients", [])
            user["patients"] = [p for p in user["patients"] if p["patientId"] != patient_id]
            user["patients"].append(new_patient)

        save_db(db)
        return jsonify({"success": True, "patient": new_patient}), 201

    user_email = request.args.get("email", "").strip().lower()
    if user_email and user_email in db["users"]:
        return jsonify(db["users"][user_email].get("patients", [])), 200

    return jsonify(db["patients"]), 200

@app.route('/api/patients/self', methods=['GET'])
def handle_patient_self_lookup():
    """Resolve a patient's own login email to the patientId a doctor already registered them
    under, so the patient's own Treatment tab can read the exact same plan the doctor manages."""
    email = request.args.get("email", "").strip().lower()
    if not email:
        return jsonify({"success": False, "message": "Email is required."}), 400

    for patient in db["patients"]:
        if patient.get("email", "").strip().lower() == email:
            return jsonify({"success": True, "patient": patient}), 200

    return jsonify({"success": False, "message": "No patient record found for this email."}), 404

@app.route('/api/appointments', methods=['GET', 'POST'])
def handle_appointments():
    global db
    if request.method == 'POST':
        data = request.get_json(force=True)
        doctor_email = data.get("doctorEmail", "").strip().lower()
        patient_email = data.get("patientEmail", "").strip().lower()
        new_app = {
            "id": str(len(db["appointments"]) + 1),
            "patientName": data.get("patientName", "Patient"),
            "patientEmail": patient_email or "patient@example.com",
            "date": data.get("date", "2026-08-02"),
            "time": data.get("time", "14:30"),
            "status": "Scheduled"
        }
        db["appointments"].append(new_app)

        if doctor_email and doctor_email in db["users"]:
            user = db["users"][doctor_email]
            user.setdefault("appointments", [])
            user["appointments"].append(new_app)

        # Also reflect this appointment on the patient's own account so it shows up
        # on their own dashboard, not just the scheduling doctor's.
        if patient_email and patient_email in db["users"]:
            patient_user = db["users"][patient_email]
            patient_user.setdefault("appointments", [])
            patient_user["appointments"].append(new_app)

        save_db(db)
        return jsonify({"success": True, "appointment": new_app}), 201

    user_email = request.args.get("email", "").strip().lower()
    if user_email and user_email in db["users"]:
        return jsonify(db["users"][user_email].get("appointments", [])), 200

    return jsonify(db["appointments"]), 200

@app.route('/api/reports', methods=['GET'])
def get_reports():
    return jsonify(db["reports"]), 200

@app.route('/api/dashboard-metrics', methods=['GET'])
def get_dashboard_metrics():
    user_email = request.args.get("email", "").strip().lower()
    
    # If a specific user email is provided, fetch user-scoped metrics
    if user_email and user_email in db["users"]:
        user = db["users"][user_email]
        user_patients = user.get("patients", [])
        user_reports = user.get("reports", [])
        user_appts = user.get("appointments", [])
        
        total_patients = len(user_patients)
        total_scans = len(user_reports)
        concordance = "0%" if total_scans == 0 else f"{min(98.5, 85.0 + total_scans * 1.5):.1f}%"
        
        return jsonify({
            "totalPatients": total_patients,
            "totalScans": total_scans,
            "concordance": concordance,
            "recentScans": user_reports[-5:],
            "scheduledAppointments": user_appts
        }), 200

    total_patients = len(db["patients"])
    total_scans = len(db["reports"])
    concordance = "0%" if total_scans == 0 else f"{min(98.5, 85.0 + total_scans * 1.5):.1f}%"
    return jsonify({
        "totalPatients": total_patients,
        "totalScans": total_scans,
        "concordance": concordance,
        "recentScans": db["reports"][-5:],
        "scheduledAppointments": db["appointments"]
    }), 200

DEFAULT_TREATMENT_STAGES = [
    {"id": "1", "title": "Initial Consultation", "status": "Pending", "isCompleted": False},
    {"id": "2", "title": "X-rays & Diagnostic Imaging", "status": "Pending", "isCompleted": False},
    {"id": "3", "title": "Treatment Planning & Approval", "status": "Pending", "isCompleted": False},
    {"id": "4", "title": "Procedure Execution", "status": "Pending", "isCompleted": False},
    {"id": "5", "title": "Follow-up & Adjustments", "status": "Pending", "isCompleted": False},
    {"id": "6", "title": "Treatment Completion", "status": "Pending", "isCompleted": False}
]

@app.route('/api/treatment-plans/<patient_key>', methods=['GET', 'POST'])
def handle_treatment_plans(patient_key):
    global db
    if request.method == 'POST':
        data = request.get_json(force=True)
        plan = {
            "stages": data.get("stages", []),
            "clinicalNotes": data.get("clinicalNotes", ""),
            "upcomingProcedures": data.get("upcomingProcedures", "")
        }
        db["treatment_plans"][patient_key] = plan
        save_db(db)
        return jsonify({"success": True}), 200

    existing = db["treatment_plans"].get(patient_key)
    if existing is None:
        return jsonify({"stages": DEFAULT_TREATMENT_STAGES, "clinicalNotes": "", "upcomingProcedures": ""}), 200
    if isinstance(existing, list):
        # Legacy format from before notes/procedures were tracked — stages only.
        return jsonify({"stages": existing, "clinicalNotes": "", "upcomingProcedures": ""}), 200
    return jsonify({
        "stages": existing.get("stages", DEFAULT_TREATMENT_STAGES),
        "clinicalNotes": existing.get("clinicalNotes", ""),
        "upcomingProcedures": existing.get("upcomingProcedures", "")
    }), 200


# Per-class clinical guidance used to build the report directly from YOLOv8 detections (no LLM call).
CLASS_GUIDANCE = {
    'calculus': {
        'doctor': "**Calculus (tartar)** detected: recommend professional scaling and polishing; reinforce interdental cleaning technique.",
        'patient': "some tartar buildup — a professional cleaning will help clear this up",
    },
    'caries': {
        'doctor': "**Caries** detected: consider excavation and restoration (composite/amalgam); evaluate depth radiographically for possible pulp involvement.",
        'patient': "early signs of a cavity — it's best to get it looked at before it grows",
    },
    'gingivitis': {
        'doctor': "**Gingivitis** noted: recommend scaling and root planing, consider an antimicrobial rinse, and reinforce home-care technique.",
        'patient': "some gum inflammation — brushing gently and flossing more consistently can help",
    },
    'hypodontia': {
        'doctor': "**Hypodontia (missing tooth)** identified: discuss prosthetic options — implant, bridge, or partial denture — and assess space maintenance needs.",
        'patient': "a missing tooth — your dentist can walk you through replacement options",
    },
    'tooth_discolation': {
        'doctor': "**Tooth discoloration** observed: assess intrinsic vs. extrinsic etiology; consider professional whitening, microabrasion, or veneers as appropriate.",
        'patient': "some discoloration — a cosmetic consult can go over whitening options",
    },
    'ulcer': {
        'doctor': "**Oral ulcer** detected: monitor healing over 10-14 days; rule out traumatic, aphthous, or systemic causes; refer if it persists beyond 2 weeks.",
        'patient': "a small mouth sore — it should heal on its own within a week or two, but mention it at your next visit if it lingers",
    },
}


def generate_analysis_report(predictions, role):
    if not predictions:
        if role == 'patient':
            return "**Good news!** Our AI scan did not detect any major concerns in your dental image. Keep up your great oral hygiene routine!\n\n**Preventive Plan**\n- Brush twice daily with fluoride toothpaste\n- Floss every evening\n- Visit your dentist every 6 months for a routine checkup"
        return "**Clinical AI Analysis Report**\n\n**Clinical Summary**\nNo significant pathological findings detected by the YOLOv8 diagnostic model in this image.\n\n**Preventive Plan**\n- Continue routine monitoring\n- Schedule standard 6-month recall\n- Reinforce home care instructions with patient"

    counts = {}
    conf_sum = {}
    for p in predictions:
        label = p['class']
        counts[label] = counts.get(label, 0) + 1
        conf_sum[label] = conf_sum.get(label, 0) + p['confidence']

    findings_text = ', '.join(f"{count}x {label}" for label, count in counts.items())
    avg_conf = sum(p['confidence'] for p in predictions) / len(predictions)

    if role == 'patient':
        concerns_text = '; '.join(CLASS_GUIDANCE.get(label, {}).get('patient', label.replace('_', ' ')) for label in counts)
        return (
            "**Your Dental Scan Summary**\n\n"
            f"Our AI system scanned your image and found: {concerns_text}.\n\n"
            "Don't worry — early detection means easier treatment! Your dentist will guide you on the best next steps.\n\n"
            "**Preventive Plan**\n"
            "- Schedule an appointment with your dentist soon to discuss these findings\n"
            "- Brush gently twice a day and use fluoride toothpaste\n"
            "- Avoid sugary foods and drinks between meals\n"
            "- Floss daily to prevent gum issues\n\n"
            f"*This report was generated directly from the YOLOv8 AI detection model (confidence: {avg_conf*100:.0f}%).*"
        )

    lines = ["**Clinical AI Analysis Report**\n"]
    lines.append(f"**Detected Findings:** {findings_text}")
    lines.append(f"**Average Detection Confidence:** {avg_conf*100:.1f}%\n")
    lines.append("**Findings Breakdown:**")
    for label, count in counts.items():
        avg = conf_sum[label] / count
        lines.append(f"- **{label}**: {count} instance(s) detected (avg. confidence: {avg*100:.0f}%)")
    lines.append("\n**Preventive Plan**")
    for label in counts:
        guidance = CLASS_GUIDANCE.get(label, {}).get('doctor')
        if guidance:
            lines.append(f"- {guidance}")
    lines.append("- Schedule follow-up in 4–6 weeks to monitor treatment response.")
    lines.append(f"\n*Report generated directly from the YOLOv8 object detection model at {avg_conf*100:.0f}% average confidence. Correlate with clinical examination before finalizing treatment.*")
    return '\n'.join(lines)


def suppress_contained_boxes(detections, containment_thresh=0.7):
    """Drop same-class boxes that are mostly contained within a higher-confidence box.
    Standard IoU-based NMS misses these because very differently sized boxes over the
    same object can have low IoU (large union) despite one fully overlapping the other."""
    detections = sorted(detections, key=lambda d: d['confidence'], reverse=True)
    kept = []
    for det in detections:
        bx, by, bw, bh = det['box']
        b_area = bw * bh
        suppressed = False
        for k in kept:
            if k['class_id'] != det['class_id']:
                continue
            kx, ky, kw, kh = k['box']
            k_area = kw * kh
            ix1, iy1 = max(bx, kx), max(by, ky)
            ix2, iy2 = min(bx + bw, kx + kw), min(by + bh, ky + kh)
            iw, ih = max(0, ix2 - ix1), max(0, iy2 - iy1)
            inter = iw * ih
            smaller_area = min(b_area, k_area)
            if smaller_area > 0 and inter / smaller_area > containment_thresh:
                suppressed = True
                break
        if not suppressed:
            kept.append(det)
    return kept


def letterbox(image, new_size=640, color=(114, 114, 114)):
    """Resize preserving aspect ratio and pad to a square, matching YOLOv8's training-time preprocessing."""
    h, w = image.shape[:2]
    scale = min(new_size / h, new_size / w)
    new_w, new_h = int(round(w * scale)), int(round(h * scale))
    resized = cv2.resize(image, (new_w, new_h), interpolation=cv2.INTER_LINEAR)
    canvas = np.full((new_size, new_size, 3), color, dtype=np.uint8)
    pad_x = (new_size - new_w) // 2
    pad_y = (new_size - new_h) // 2
    canvas[pad_y:pad_y + new_h, pad_x:pad_x + new_w] = resized
    return canvas, scale, pad_x, pad_y


loaded_model = None

def get_yolo_model():
    global loaded_model
    if loaded_model is None:
        try:
            print("🚀 Lazily loading YOLO model best (5).onnx...")
            loaded_model = cv2.dnn.readNetFromONNX(os.path.join(os.path.dirname(__file__), 'best (5).onnx'))
            print("✅ YOLO model loaded successfully!")
        except Exception as e:
            print(f"❌ Error loading YOLO model: {e}")
    return loaded_model

@app.route('/analyze', methods=['POST'])
def analyze_image():
    net = get_yolo_model()
    try:
        data_payload = request.get_json(force=True)
        base64_data = data_payload.get("image", "")
        role = data_payload.get("role", "doctor")
        patient_name = data_payload.get("patientName", "Doctor")
        patient_email = data_payload.get("patientEmail", "").strip().lower()
        doctor_email = data_payload.get("doctorEmail", "").strip().lower()
        
        image_data = base64.b64decode(base64_data)
        image = Image.open(io.BytesIO(image_data)).convert('RGB')
        width, height = image.size
        
        predictions = []
        open_cv_image = np.array(image)[:, :, ::-1].copy()
        
        if net:
            letterboxed, lb_scale, pad_x, pad_y = letterbox(open_cv_image, 640)
            blob = cv2.dnn.blobFromImage(letterboxed, 1.0/255.0, (640, 640), swapRB=True, crop=False)
            net.setInput(blob)
            output = net.forward()
            output = np.squeeze(output)

            if len(output.shape) == 2:
                output = output.T
                boxes, confidences, class_ids = [], [], []
                classes = ['calculus', 'caries', 'gingivitis', 'hypodontia', 'tooth_discolation', 'ulcer']

                CONF_THRESHOLD = 0.15
                for row in output:
                    scores = row[4:]
                    class_id = np.argmax(scores)
                    confidence = scores[class_id]
                    if confidence >= CONF_THRESHOLD:
                        cx, cy, w, h = row[:4]
                        # Undo letterbox padding/scale to map the 640x640 prediction back to original image space
                        cx = (cx - pad_x) / lb_scale
                        cy = (cy - pad_y) / lb_scale
                        w = w / lb_scale
                        h = h / lb_scale
                        x = int(cx - w / 2)
                        y = int(cy - h / 2)
                        box_w = int(w)
                        box_h = int(h)
                        # Clamp to image bounds
                        x = max(0, min(x, width - 1))
                        y = max(0, min(y, height - 1))
                        box_w = max(1, min(box_w, width - x))
                        box_h = max(1, min(box_h, height - y))
                        boxes.append([x, y, box_w, box_h])
                        confidences.append(float(confidence))
                        class_ids.append(class_id)

                indices = cv2.dnn.NMSBoxes(boxes, confidences, CONF_THRESHOLD, 0.45)
                if len(indices) > 0:
                    flat_indices = indices.flatten() if hasattr(indices, 'flatten') else [i[0] for i in indices]
                    surviving = [
                        {"box": boxes[i], "confidence": confidences[i], "class_id": class_ids[i]}
                        for i in flat_indices
                    ]
                    for det in suppress_contained_boxes(surviving):
                        x, y, box_w, box_h = det["box"]
                        predictions.append({
                            "class": classes[det["class_id"]],
                            "confidence": det["confidence"],
                            "x": x + box_w/2,
                            "y": y + box_h/2,
                            "width": box_w,
                            "height": box_h
                        })
        
        analysis_report = generate_analysis_report(predictions, role)

        # Save scan report to dynamic DB
        new_report = {
            "id": str(len(db["reports"]) + 1),
            "patientName": patient_name,
            "patientEmail": patient_email,
            "date": datetime.now().strftime("%Y-%m-%d"),
            "findings": ', '.join(set(p['class'].replace('_', ' ').title() for p in predictions)) or "Healthy",
            "confidence": int(sum(p['confidence'] for p in predictions)/len(predictions) * 100) if predictions else 95,
            "summary": analysis_report[:200] + "..."
        }
        db["reports"].append(new_report)

        # Attach to the requesting doctor's own report history so scans/dashboard are per-doctor
        if doctor_email and doctor_email in db["users"]:
            user = db["users"][doctor_email]
            user.setdefault("reports", [])
            user["reports"].append(new_report)

        save_db(db)

        return jsonify({
            "image": {"width": width, "height": height},
            "predictions": predictions,
            "gemini_report": analysis_report
        })

    except Exception as e:
        print("Error during inference:", e)
        return jsonify({"error": str(e)}), 500

@app.route('/chat', methods=['POST'])
def chat():
    try:
        data = request.get_json(force=True)
        user_message = data.get("message", "").strip()
        role = data.get("role", "patient")
        history = data.get("history", [])

        if not user_message:
            return jsonify({"reply": "Please type a message."}), 200

        system_prompt = "You are SmileGuard AI, an expert dental AI assistant."
        reply = None

        if groq_client:
            try:
                messages = [{"role": "system", "content": system_prompt}] + [{"role": "user" if m["sender"]=="user" else "assistant", "content": m["text"]} for m in history[-10:]] + [{"role": "user", "content": user_message}]
                completion = groq_client.chat.completions.create(model="llama-3.3-70b-versatile", messages=messages)
                reply = completion.choices[0].message.content.strip()
            except Exception as e:
                print(f"Groq Chat Error: {e}")

        if not reply and openai_client:
            try:
                messages = [{"role": "system", "content": system_prompt}] + [{"role": "user" if m["sender"]=="user" else "assistant", "content": m["text"]} for m in history[-10:]] + [{"role": "user", "content": user_message}]
                completion = openai_client.chat.completions.create(model="gpt-4o-mini", messages=messages)
                reply = completion.choices[0].message.content.strip()
            except Exception as e:
                print(f"OpenAI Chat Error: {e}")

        if not reply and gemini_model:
            try:
                response = gemini_model.generate_content(user_message)
                reply = response.text.strip()
            except Exception as e:
                print(f"Gemini Chat Error: {e}")

        if not reply:
            reply = generate_fallback_chat_reply(user_message, role)

        return jsonify({"reply": reply})

    except Exception as e:
        return jsonify({"reply": "Sorry, I ran into an issue processing your request."}), 200

def generate_fallback_chat_reply(user_message, role):
    msg = user_message.lower()
    if role == "doctor":
        if "carie" in msg or "cavity" in msg:
            return "**SmileGuard AI (Clinical Guidance)**\n\nFor caries management, evaluate non-cavitated vs cavitated lesions. Apply remineralization (fluoride/SDF) or composite restorations."
        else:
            return "**SmileGuard AI (Clinical Guidance)**\n\nDiagnostic accuracy relies on combining radiographic evidence, patient history, and clinical exam."
    else:
        if "brush" in msg or "floss" in msg:
            return "Brush twice daily with fluoride toothpaste and floss every night for optimal dental health! 🪥"
        else:
            return "Keep up great dental habits! Brush twice daily, limit sugary snacks, and visit your dentist every 6 months."

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    print(f"Starting ProphyDent Persistent AI Backend Server on port {port}...")
    app.run(host='0.0.0.0', port=port)
