import { useState, useEffect } from 'react'
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import './App.css'
const API_BASE_URL = import.meta.env.VITE_JAVA_API_URL;

function App() {
  // --- AUTHENTICATION STATE ---
  const [token, setToken] = useState(localStorage.getItem('jwt') || '');
  const [userId, setUserId] = useState(localStorage.getItem('userId') || '');
  const [userName, setUserName] = useState(localStorage.getItem('userName') || '');
  
  // --- UI TOGGLE STATE ---
  const [isRegistering, setIsRegistering] = useState(false);
  const [authEmail, setAuthEmail] = useState('');
  const [authPassword, setAuthPassword] = useState('');
  const [authName, setAuthName] = useState('');
  const [authMessage, setAuthMessage] = useState({ text: '', type: '' });

  // --- APP STATE ---
  const [expenses, setExpenses] = useState([]);
  const [showChart, setShowChart] = useState(false);
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('Other');
  // NEW: Date state (defaults to today in YYYY-MM-DD format)
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]); 
  const [editingId, setEditingId] = useState(null);

  const [isScanning, setIsScanning] = useState(false); // Tracks the AI thinking time

  const [isLoading, setIsLoading] = useState(false);
  const [isDarkMode, setIsDarkMode] = useState(localStorage.getItem('theme') === 'dark');
  
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const CATEGORIES = ['Food', 'Housing', 'Transport', 'Subscriptions', 'Utilities', 'Other'];
  const COLORS = ['#10b981', '#3b82f6', '#f59e0b', '#ef4444', '#8b5cf6', '#64748b'];

// NEW: Watch for theme changes and apply them to the HTML body
  useEffect(() => {
    if (isDarkMode) {
      document.body.classList.add('dark-mode');
      localStorage.setItem('theme', 'dark');
    } else {
      document.body.classList.remove('dark-mode');
      localStorage.setItem('theme', 'light');
    }
  }, [isDarkMode]);

  useEffect(() => {
    if (token && userId) fetchExpenses();
  }, [token, userId, currentPage]); 

  useEffect(() => {
    if (editingId) return; 
    const text = description.toLowerCase();
    if (text.includes('zomato') || text.includes('swiggy') || text.includes('grocery') || text.includes('food')) setCategory('Food');
    else if (text.includes('rent') || text.includes('emi')) setCategory('Housing');
    else if (text.includes('uber') || text.includes('petrol') || text.includes('metro')) setCategory('Transport');
    else if (text.includes('netflix') || text.includes('amazon') || text.includes('aws') || text.includes('spotify')) setCategory('Subscriptions');
    else if (text.includes('wifi') || text.includes('electricity') || text.includes('water')) setCategory('Utilities');
  }, [description]);

  // --- SECURITY LOGIC ---
  const handleAuthSubmit = async (e) => {
    e.preventDefault();
    setAuthMessage({ text: '', type: '' });
    const url = isRegistering ? `${API_BASE_URL}/api/users/register` : `${API_BASE_URL}/api/users/login`;
    const payload = isRegistering ? { name: authName, email: authEmail, password: authPassword } : { email: authEmail, password: authPassword };

    try {
      const response = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
      if (response.ok) {
        if (isRegistering) {
          setAuthMessage({ text: 'Registration successful! Please log in.', type: 'success' });
          setIsRegistering(false); setAuthPassword('');
        } else {
          const data = await response.json();
          setToken(data.token); setUserId(data.userId); setUserName(data.name);
          localStorage.setItem('jwt', data.token); localStorage.setItem('userId', data.userId); localStorage.setItem('userName', data.name);
        }
      } else {
        setAuthMessage({ text: isRegistering ? 'Registration failed.' : 'Invalid email or password.', type: 'error' });
      }
    } catch (error) { setAuthMessage({ text: 'Unable to connect to server.', type: 'error' }); }
  };

  const handleLogout = () => {
    setToken(''); setUserId(''); setUserName('');
    localStorage.clear(); setExpenses([]); resetForm();
  };

  const resetForm = () => {
    setDescription(''); setAmount(''); setCategory('Other'); setEditingId(null);
    setDate(new Date().toISOString().split('T')[0]); // Reset to today
  };

  // --- OCR FILE UPLOAD LOGIC ---
  const handleReceiptUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setIsScanning(true); // Start the loading animation
    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetch(`${API_BASE_URL}/api/expenses/upload-receipt`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }, // Bypassing the Java bouncer!
        body: formData
      });

      if (response.ok) {
        const data = await response.json();
        
        // AUTO-FILL THE FORM WITH SMART AI DATA!
        setDescription(data.description);
        setAmount(data.amount);
        setCategory(data.category);
        
        // NEW: If the AI found a date, set it! Otherwise, leave it as today.
        if (data.date) {
            setDate(data.date);
        }
      } else {
        alert("Failed to scan receipt.");
      }
    } catch (err) {
      console.error(err);
    } finally {
      setIsScanning(false);
      e.target.value = ''; // Reset the file input so you can upload the same file again if needed
    }
  };

  // --- CRUD API CALLS ---
  const fetchExpenses = () => {
    setIsLoading(true);
    // NEW: Appending the page and size to the URL!
    fetch(`${API_BASE_URL}/api/expenses/user/${userId}?page=${currentPage}&size=20`, { 
      headers: { 'Authorization': `Bearer ${token}` } 
    })
      .then(res => {
        if (res.status === 401 || res.status === 403) throw new Error("Session Expired");
        if (!res.ok) throw new Error("Failed to fetch");
        return res.json();
      })
      .then(data => {
        // NEW: Spring Boot puts the array inside "content" now!
        setExpenses(data.content || []); 
        setTotalPages(data.totalPages || 1);
        setIsLoading(false);
      })
      .catch(err => {
        console.error(err);
        setIsLoading(false);
        if (err.message === "Session Expired") {
          handleLogout();
          setAuthMessage({ text: 'Your session has expired. Please log in again.', type: 'error' });
        }
      });
  };

  const handleSaveExpense = async (e) => {
    e.preventDefault();
    // NEW: Sending the date to Java!
    const expenseData = { description, amount: parseFloat(amount), category, date, user: { id: userId } };
    const method = editingId ? 'PUT' : 'POST';
    const url = editingId ? `${API_BASE_URL}/api/expenses/${editingId}` : `${API_BASE_URL}/api/expenses`;

    try {
      const response = await fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
        body: JSON.stringify(expenseData)
      });
      if (response.ok) { resetForm(); fetchExpenses(); }
    } catch (error) { console.error("Failed to save", error); }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Are you sure you want to delete this expense?")) return;
    try {
      const response = await fetch(`${API_BASE_URL}/api/expenses/${id}`, { method: 'DELETE', headers: { 'Authorization': `Bearer ${token}` } });
      if (response.ok) fetchExpenses();
    } catch (error) { console.error("Failed to delete", error); }
  };

  const handleEditClick = (expense) => {
    setDescription(expense.description); setAmount(expense.amount); setCategory(expense.category); setEditingId(expense.id);
    setDate(expense.date || new Date().toISOString().split('T')[0]); // Load old date
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // --- DASHBOARD MATH & SORTING ---
  const totalSpent = expenses.reduce((sum, exp) => sum + exp.amount, 0);
  
  const getTopCategory = () => {
    if (expenses.length === 0) return "N/A";
    const totals = expenses.reduce((acc, exp) => {
      acc[exp.category] = (acc[exp.category] || 0) + exp.amount; return acc;
    }, {});
    return Object.keys(totals).reduce((a, b) => totals[a] > totals[b] ? a : b);
  };

  const getChartData = () => {
    const categoryTotals = expenses.reduce((acc, expense) => {
      acc[expense.category] = (acc[expense.category] || 0) + expense.amount; return acc;
    }, {});
    return Object.keys(categoryTotals).map(key => ({ name: key, value: categoryTotals[key] }));
  };

  // NEW: Sort expenses chronologically (Newest first)
  const sortedExpenses = [...expenses].sort((a, b) => {
    const dateA = new Date(a.date || '1970-01-01');
    const dateB = new Date(b.date || '1970-01-01');
    return dateB - dateA; 
  });

  // --- RENDERING LOGIC ---
  if (!token) {
    return (
      <div className="login-container">
        <div className="login-card">
          <h2>{isRegistering ? "Create Vault" : "Welcome Back"}</h2>
          <form className="login-form" onSubmit={handleAuthSubmit}>
            {authMessage.text && <div className="error-text" style={{ color: authMessage.type === 'success' ? '#10b981' : '#ef4444' }}>{authMessage.text}</div>}
            {isRegistering && <input type="text" className="input-field" placeholder="Full Name" value={authName} onChange={(e) => setAuthName(e.target.value)} required={isRegistering} />}
            <input type="email" className="input-field" placeholder="Email" value={authEmail} onChange={(e) => setAuthEmail(e.target.value)} required />
            <input type="password" className="input-field" placeholder="Password" value={authPassword} onChange={(e) => setAuthPassword(e.target.value)} required />
            <button type="submit" className="submit-btn">{isRegistering ? "Sign Up" : "Unlock Vault"}</button>
          </form>
          <button className="logout-btn" style={{ marginTop: '20px', border: 'none', width: '100%' }} onClick={() => { setIsRegistering(!isRegistering); setAuthMessage({ text: '', type: '' }); }}>
            {isRegistering ? "Already have an account? Log in." : "Don't have an account? Sign up."}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard-container">
      <div className="header-row">
        <div>
          <h1 className="dashboard-header" style={{ marginBottom: 0 }}>Finance Tracker</h1> 
          <p style={{ color: 'var(--text-muted)', marginTop: '4px' }}>Logged in as {userName}</p>
        </div>
        <div style={{ display: 'flex', gap: '12px' }}>
          <button className="toggle-btn" style={{ margin: 0 }} onClick={() => setIsDarkMode(!isDarkMode)}>
            {isDarkMode ? '☀️ Light' : '🌙 Dark'}
          </button>
          <button className="logout-btn" onClick={handleLogout}>Log Out</button>
        </div>
      </div>

      {/* --- NEW EXECUTIVE SUMMARY CARDS --- */}
      <div className="metrics-grid">
        <div className="metric-card">
          <span className="metric-title">Total Spent</span>
          <span className="metric-value">₹{totalSpent.toLocaleString('en-IN')}</span>
        </div>
        <div className="metric-card">
          <span className="metric-title">Top Category</span>
          <span className="metric-value" style={{ color: '#3b82f6' }}>{getTopCategory()}</span>
        </div>
        <div className="metric-card">
          <span className="metric-title">Transactions</span>
          <span className="metric-value">{expenses.length}</span>
        </div>
      </div>
      
      {/* --- NEW RECEIPT SCANNER BUTTON --- */}
      <div style={{ marginBottom: '16px', textAlign: 'center' }}>
        <input 
          type="file" 
          id="receipt-upload" 
          accept="image/*" 
          style={{ display: 'none' }} 
          onChange={handleReceiptUpload} 
        />
        <label htmlFor="receipt-upload" className="toggle-btn" style={{ display: 'inline-block', width: 'auto', background: isScanning ? 'var(--card-bg)' : 'transparent' }}>
          {isScanning ? "⏳ AI is reading receipt..." : "📸 Upload Receipt Image"}
        </label>
      </div>

      {/* --- UPGRADED EXPENSE FORM (Now with Date Picker) --- */}
      <div className="expense-form-card">
        <form className="expense-form" onSubmit={handleSaveExpense}>
          <input type="text" className="input-field desc-input" placeholder="What did you buy?" value={description} onChange={(e) => setDescription(e.target.value)} required />
          <input type="number" className="input-field amount-input" placeholder="Amount (₹)" value={amount} onChange={(e) => setAmount(e.target.value)} required />
          <select className="input-field" value={category} onChange={(e) => setCategory(e.target.value)} style={{ padding: '12px' }}>
            {CATEGORIES.map(cat => <option key={cat} value={cat}>{cat}</option>)}
          </select>
          <input type="date" className="input-field" value={date} onChange={(e) => setDate(e.target.value)} required style={{ padding: '12px' }}/>

          <div className="form-actions">
            {/* We only force the blue color if we are editing. Otherwise, let CSS handle it! */}
            <button type="submit" className="submit-btn" style={editingId ? { background: '#3b82f6', color: 'white' } : {}}>
              {editingId ? "Update" : "Add Expense"}
            </button>
            {editingId && <button type="button" className="logout-btn" onClick={resetForm}>Cancel</button>}
          </div>
        </form>
      </div>

      {expenses.length > 0 && (
        <div style={{ marginBottom: '24px' }}>
          <button className="toggle-btn" onClick={() => setShowChart(!showChart)}>
            {showChart ? "Hide Spending Chart" : "Show Spending Chart"}
          </button>
          {showChart && (
            <div className="expense-form-card" style={{ height: '320px', paddingBottom: '40px', marginTop: '12px' }}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={getChartData()} cx="50%" cy="50%" innerRadius={70} outerRadius={90} paddingAngle={5} dataKey="value" stroke="none">
                    {getChartData().map((entry, index) => <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />)}
                  </Pie>
                  <Tooltip formatter={(value) => `₹${value.toLocaleString('en-IN')}`} />
                  <Legend verticalAlign="bottom" height={36}/>
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
      )}

      {/* --- UPGRADED TRANSACTION LIST (Sorted & Dated) --- */}
      <h3 className="section-title">Recent Transactions</h3>
      {isLoading ? (
        <div className="spinner"></div> // Show spinner if loading
      ) : expenses.length === 0 ? (
        <p style={{ color: 'var(--text-muted)' }}>No transactions yet.</p>
      ) : (
        <ul className="expense-list">
          {/* NOTICE: We are mapping over sortedExpenses now, not expenses! */}
          {sortedExpenses.map(expense => (
            <li key={expense.id} className="expense-item" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div className="expense-info" style={{ flexGrow: 1 }}>
                <span className="expense-desc">{expense.description}</span>
                <span className="expense-category" style={{ fontSize: '0.8rem', color: '#a1a1aa' }}>
                  {/* Shows the Date and the Category! */}
                  {expense.date ? new Date(expense.date).toLocaleDateString('en-IN', { month: 'short', day: 'numeric', year: 'numeric' }) : 'Unknown Date'} • {expense.category}
                </span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
                <span className="expense-amount" style={{ fontWeight: 'bold' }}>₹{expense.amount.toLocaleString('en-IN')}</span>
                <div className="action-buttons">
                  <button onClick={() => handleEditClick(expense)} style={{ background: 'none', border: 'none', color: '#3b82f6', cursor: 'pointer', fontSize: '0.9rem' }}>Edit</button>
                  <button onClick={() => handleDelete(expense.id)} style={{ background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer', fontSize: '0.9rem' }}>Delete</button>
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}

      {/* --- NEW PAGINATION CONTROLS --- */}
      {expenses.length > 0 && totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '20px' }}>
          <button 
            className="toggle-btn" 
            style={{ width: 'auto', margin: 0, opacity: currentPage === 0 ? 0.5 : 1 }} 
            disabled={currentPage === 0} 
            onClick={() => setCurrentPage(c => c - 1)}
          >
            &larr; Previous
          </button>
          
          <span style={{ color: 'var(--text-muted)', fontSize: '0.9rem', fontWeight: 500 }}>
            Page {currentPage + 1} of {totalPages}
          </span>
          
          <button 
            className="toggle-btn" 
            style={{ width: 'auto', margin: 0, opacity: currentPage >= totalPages - 1 ? 0.5 : 1 }} 
            disabled={currentPage >= totalPages - 1} 
            onClick={() => setCurrentPage(c => c + 1)}
          >
            Next &rarr;
          </button>
        </div>
      )}
    </div>
  );
}

export default App;