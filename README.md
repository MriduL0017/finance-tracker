# Transaction Vault

Transaction Vault is a full-stack, AI-powered personal finance tracker application. It helps users manage their expenses, track transactions, and gain intelligent insights into their financial habits using a modern microservices architecture.

## 🏗 Architecture & Tech Stack

The project is divided into three primary components:

### 1. Backend (`/tracker`)
A robust REST API that handles user authentication, data persistence, and core business logic.
- **Language**: Java 17
- **Framework**: Spring Boot (v4.0.3)
- **Security**: Spring Security + JWT Authentication
- **Database ORM**: Spring Data JPA
- **Database**: PostgreSQL
- **Build Tool**: Maven

### 2. Frontend (`/ai-finance-ui`)
A fast, responsive, and dynamic user interface for managing finances and visualizing spending patterns.
- **Core**: React 19
- **Build Tool**: Vite
- **Data Visualization**: Recharts
- **Styling**: Modern CSS

### 3. AI Service (`/ai-finance-service`)
A dedicated Python microservice leveraging generative AI for advanced transaction insights, such as receipt processing and categorization.
- **Framework**: FastAPI
- **AI Integration**: Google GenAI
- **Image Processing**: Pillow
- **Server**: Uvicorn

---

## 🚀 Getting Started

### Prerequisites
Before running the application, ensure you have the following installed:
- **Java 17+**
- **Maven**
- **Node.js** (v18+) & **npm**
- **Python 3.8+**
- **Docker & Docker Compose** (Optional, for simplified deployment)
- **PostgreSQL** (If running locally without Docker)

### Option A: Running with Docker Compose
The project includes a `docker-compose.yml` file to quickly spin up the database, AI service, and Java backend.

1. Create a `.env` file in the root directory and configure the necessary variables (e.g., `DB_PASS`, `GEMINI_API_KEY`).
2. Run the following command:
   ```bash
   docker-compose up --build
   ```
3. Start the React frontend manually (see step 4 below).

### Option B: Running Manually

#### 1. Setup PostgreSQL Database
Ensure your local PostgreSQL server is running. Create a database named `finance_tracker`. Update `tracker/src/main/resources/application.properties` with your database credentials if needed.

#### 2. Start the AI Service
Open a new terminal and navigate to the AI service folder:
```bash
cd ai-finance-service
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```
*(Make sure you have your `.env` file configured in this directory for your Google GenAI key)*

#### 3. Start the Spring Boot Backend
Open a new terminal and navigate to the backend folder:
```bash
cd tracker
mvn clean install
mvn spring-boot:run
```
The backend API will start on `http://localhost:8080`.

#### 4. Start the React Frontend
Open a new terminal and navigate to the UI folder:
```bash
cd ai-finance-ui
npm install
npm run dev
```
The React development server will start on `http://localhost:5173`.

---

## ✨ Features
- **Secure Authentication**: Robust JWT-based login and signup.
- **Transaction Management**: Easily add, view, and organize financial transactions.
- **AI-Powered Insights**: Smart analysis of your spending using Google GenAI.
- **Interactive Dashboards**: Visualize your data effortlessly with Recharts.

## 🤝 Contributing
Feel free to submit issues, fork the repository, and send pull requests!
